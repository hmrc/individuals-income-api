/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.individualsincomeapi.connector

import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.UpstreamErrorResponse.Upstream5xxResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPaye, IfPayeEntry, IfSa, IfSaEntry}
import uk.gov.hmrc.individualsincomeapi.util.Interval
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class IfConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient, val auditHelper: AuditHelper)
    extends Logging {

  private val serviceUrl = servicesConfig.baseUrl("integration-framework")

  private lazy val integrationFrameworkBearerToken = servicesConfig.getString(
    "microservice.services.integration-framework.authorization-token"
  )

  private lazy val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  def fetchPayeIncome(nino: Nino, interval: Interval, filter: Option[String], matchId: String)(
    implicit hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext): Future[Seq[IfPayeEntry]] = {
    val startDate = interval.getStart.toLocalDate
    val endDate = interval.getEnd.toLocalDate

    val payeUrl = s"$serviceUrl/individuals/income/paye/" +
      s"nino/$nino?startDate=$startDate&endDate=$endDate${filter.map(f => s"&fields=$f").getOrElse("")}"

    callPaye(payeUrl, matchId)

  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval, filter: Option[String], matchId: String)(
    implicit hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext): Future[Seq[IfSaEntry]] = {
    val startYear = taxYearInterval.fromTaxYear.endYr
    val endYear = taxYearInterval.toTaxYear.endYr
    val saUrl = s"$serviceUrl/individuals/income/sa/" +
      s"nino/$nino?startYear=$startYear&endYear=$endYear${filter.map(f => s"&fields=$f").getOrElse("")}"

    callSa(saUrl, matchId)

  }

  private def extractCorrelationId(requestHeader: RequestHeader) =
    requestHeader.headers.get("CorrelationId") match {
      case Some(uuidString) =>
        Try(UUID.fromString(uuidString)) match {
          case Success(_) => uuidString
          case _          => throw new BadRequestException("Malformed CorrelationId")
        }
      case None => throw new BadRequestException("CorrelationId is required")
    }

  private def setHeaders(requestHeader: RequestHeader) = Seq(
    HeaderNames.authorisation -> s"Bearer $integrationFrameworkBearerToken",
    "Environment"             -> integrationFrameworkEnvironment,
    "CorrelationId"           -> extractCorrelationId(requestHeader)
  )

  private def callPaye(
    url: String,
    matchId: String)(implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) =
    recover[IfPayeEntry](
      http.GET[IfPaye](url, headers = setHeaders(request)) map { response =>
        auditHelper.auditIfPayeApiResponse(extractCorrelationId(request), matchId, request, url, response.paye)

        response.paye
      },
      extractCorrelationId(request),
      matchId,
      request,
      url
    )

  private def callSa(
    url: String,
    matchId: String)(implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) =
    recover[IfSaEntry](
      http.GET[IfSa](url, headers = setHeaders(request)) map { response =>
        auditHelper.auditIfSaApiResponse(extractCorrelationId(request), matchId, request, url, response.sa)

        response.sa
      },
      extractCorrelationId(request),
      matchId,
      request,
      url
    )

  private def recover[A](
    x: Future[Seq[A]],
    correlationId: String,
    matchId: String,
    request: RequestHeader,
    requestUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[A]] = x.recoverWith {
    case validationError: JsValidationException =>
      logger.warn("Integration Framework JsValidationException encountered")
      auditHelper.auditIfApiFailure(
        correlationId,
        matchId,
        request,
        requestUrl,
        s"Error parsing IF response: ${validationError.errors}")
      Future.failed(new InternalServerException("Something went wrong."))

    case UpstreamErrorResponse(msg, 404, _, _) =>
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)

      if (msg.contains("NO_DATA_FOUND")) {
        Future.successful(Seq.empty)
      } else {
        logger.warn("Integration Framework NotFoundException encountered")
        Future.failed(new NotFoundException(msg))
      }
    case Upstream5xxResponse(UpstreamErrorResponse(msg, code, _, _)) =>
      logger.warn(s"Integration Framework Upstream5xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"Internal Server error: $msg")
      Future.failed(new InternalServerException("Something went wrong."))
    case UpstreamErrorResponse(msg, 429, _, _) =>
      logger.warn(s"IF Rate limited: $msg")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    case UpstreamErrorResponse(msg, code, _, _) =>
      logger.warn(s"Integration Framework Upstream4xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)
      Future.failed(new InternalServerException("Something went wrong."))
    case e: Exception =>
      logger.error(s"Integration Framework Exception encountered", e)
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, e.getMessage)
      Future.failed(new InternalServerException("Something went wrong."))
  }
}
