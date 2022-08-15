/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.Interval
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPaye, IfPayeEntry, IfSa, IfSaEntry}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class IfConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient, val auditHelper: AuditHelper) {

  val logger: Logger = Logger(getClass)

  val serviceUrl = servicesConfig.baseUrl("integration-framework")

  lazy val integrationFrameworkBearerToken = servicesConfig.getString(
    "microservice.services.integration-framework.authorization-token"
  )

  lazy val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  def fetchPayeIncome(nino: Nino, interval: Interval, filter: Option[String], matchId: String)
                     (implicit hc: HeaderCarrier,
                      request: RequestHeader,
                      ec: ExecutionContext): Future[Seq[IfPayeEntry]] = {

    val endpoint = "IfConnector::fetchPayeIncome"

    val startDate = interval.getStart.toLocalDate
    val endDate = interval.getEnd.toLocalDate

    val payeUrl = s"$serviceUrl/individuals/income/paye/" +
      s"nino/$nino?startDate=$startDate&endDate=$endDate${filter.map(f => s"&fields=$f").getOrElse("")}"

    callPaye(payeUrl, endpoint, matchId)

  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval, filter: Option[String], matchId: String)
                               (implicit hc: HeaderCarrier,
                                request: RequestHeader,
                                ec: ExecutionContext): Future[Seq[IfSaEntry]] = {

    val endpoint = "IfConnector::fetchSelfAssessmentIncome"

    val startYear = taxYearInterval.fromTaxYear.endYr
    val endYear = taxYearInterval.toTaxYear.endYr
    val saUrl = s"$serviceUrl/individuals/income/sa/" +
      s"nino/$nino?startYear=$startYear&endYear=$endYear${filter.map(f => s"&fields=$f").getOrElse("")}"

    callSa(saUrl, endpoint, matchId)

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

  def setHeaders(requestHeader: RequestHeader) = Seq(
    HeaderNames.authorisation -> s"Bearer $integrationFrameworkBearerToken",
    "Environment"             -> integrationFrameworkEnvironment,
    "CorrelationId"           -> extractCorrelationId(requestHeader)
  )

  private def callPaye(url: String, endpoint: String, matchId: String)
                      (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) = {
    recover[IfPayeEntry](http.GET[IfPaye](url, headers = setHeaders(request)) map {
      response =>
        auditHelper.auditIfPayeApiResponse(
          extractCorrelationId(request),
          matchId, request, url, response.paye)

        response.paye
    },
    extractCorrelationId(request), matchId, request, url)((x: String) => {
      logger.warn("No NO_DATA_FOUND found, but treating as no data found")
      Future.successful(Seq.empty[IfPayeEntry])
    }
    )
  }

  private def callSa(url: String, endpoint: String, matchId: String)
                    (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) = {
    recover[IfSaEntry](http.GET[IfSa](url, headers = setHeaders(request)) map {
      response =>
        auditHelper.auditIfSaApiResponse(
          extractCorrelationId(request),
          matchId, request, url, response.sa)

        response.sa
    },
    extractCorrelationId(request), matchId, request, url) { (message: String) =>
      logger.warn("Integration Framework NotFoundException encountered")
      Future.failed(new NotFoundException(message))
    }
  }

  private def recover[A](x: Future[Seq[A]],
                         correlationId: String,
                         matchId: String,
                         request: RequestHeader,
                         requestUrl: String)(default404: (String) => Future[Seq[A]])
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[A]] = x.recoverWith {
    case validationError: JsValidationException => {
      logger.warn("Integration Framework JsValidationException encountered")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl,
        s"Error parsing IF response: ${validationError.errors}")
      Future.failed(new InternalServerException("Something went wrong."))
    }

    case Upstream4xxResponse(msg, 404, _, _) => {
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)

      msg.contains("NO_DATA_FOUND") match {
        case true => Future.successful(Seq.empty)
        case _    => default404(msg)
      }
    }
    case Upstream5xxResponse(msg, code, _, _) => {
      logger.warn(s"Integration Framework Upstream5xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"Internal Server error: $msg")
      Future.failed(new InternalServerException("Something went wrong."))
    }
    case Upstream4xxResponse(msg, 429, _, _) => {
      logger.warn(s"IF Rate limited: $msg")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    }
    case Upstream4xxResponse(msg, code, _, _) => {
      logger.warn(s"Integration Framework Upstream4xxResponse encountered: $code")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, msg)
      Future.failed(new InternalServerException("Something went wrong."))
    }
    case e: Exception => {
      logger.warn(s"Integration Framework Exception encountered")
      auditHelper.auditIfApiFailure(correlationId, matchId, request, requestUrl, e.getMessage)
      Future.failed(new InternalServerException("Something went wrong."))
    }
  }
}
