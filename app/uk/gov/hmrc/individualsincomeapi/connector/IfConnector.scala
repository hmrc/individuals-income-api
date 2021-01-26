/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.joda.time.Interval
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, NotFoundException,
  TooManyRequestException, Upstream4xxResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.audit.v2.models.{ApiIfAuditRequest, ApiIfFailureAuditRequest}
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPaye, IfPayeEntry, IfSa, IfSaEntry}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class IfConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient, val auditHelper: AuditHelper)
                           (implicit ec: ExecutionContext) {

  val serviceUrl = servicesConfig.baseUrl("integration-framework")

  lazy val integrationFrameworkBearerToken = servicesConfig.getString(
    "microservice.services.integration-framework.authorization-token"
  )

  lazy val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  def fetchPayeIncome(nino: Nino, interval: Interval, filter: Option[String], matchId: String)
                     (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext): Future[Seq[IfPayeEntry]] = {

    val endpoint = "IfConnector::fetchPayeIncome"

    val startDate = interval.getStart.toLocalDate
    val endDate = interval.getEnd.toLocalDate
    val payeUrl = s"$serviceUrl/individuals/income/paye/" +
      s"nino/$nino?startDate=$startDate&endDate=$endDate${filter.map(f => s"&fields=$f").getOrElse("")}"

    callPaye(payeUrl, endpoint, matchId)

  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval, filter: Option[String], matchId: String)
                               (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext): Future[Seq[IfSaEntry]] = {

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

  private def header(extraHeaders: (String, String)*)
                    (implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer $integrationFrameworkBearerToken")))
      .withExtraHeaders(Seq("Environment" -> integrationFrameworkEnvironment) ++ extraHeaders: _*)

  private def callPaye(url: String, endpoint: String, matchId: String)
                      (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) =
    recover[IfPayeEntry](http.GET[IfPaye](url)(implicitly, header(), ec) map {
      response =>
        Logger.debug(s"$endpoint - Response: $response")

        auditHelper.auditIfApiResponse(
          ApiIfAuditRequest(extractCorrelationId(request), None, Some(matchId), request, url, Json.toJson(response))
        )

        response.paye
      },
      ApiIfFailureAuditRequest(extractCorrelationId(request), None, None, request, url))

  private def callSa(url: String, endpoint: String, matchId: String)
                    (implicit hc: HeaderCarrier, request: RequestHeader, ec: ExecutionContext) =
    recover[IfSaEntry](http.GET[IfSa](url)(implicitly, header(), ec) map {
      response =>
        Logger.debug(s"$endpoint - Response: $response")

        auditHelper.auditIfApiResponse(
          ApiIfAuditRequest(extractCorrelationId(request), None, Some(matchId), request, url, Json.toJson(response))
        )

        response.sa
      },
      ApiIfFailureAuditRequest(extractCorrelationId(request), None, Some(matchId), request, url)
    )

  private def recover[A](x: Future[Seq[A]], apiIfFailedAuditRequest: ApiIfFailureAuditRequest)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[A]] = x.recoverWith {
    case notFound: NotFoundException => {
      auditHelper.auditIfApiFailure(apiIfFailedAuditRequest, notFound.getMessage)
      Future.successful(Seq.empty)
    }
    case Upstream4xxResponse(msg, 429, _, _) => {
      Logger.warn(s"IF Rate limited: $msg")
      auditHelper.auditIfApiFailure(apiIfFailedAuditRequest, s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    }
    case Upstream4xxResponse(msg, _, _, _) => {
      auditHelper.auditIfApiFailure(apiIfFailedAuditRequest, msg)
      Future.failed(new IllegalArgumentException(s"Integration Framework returned INVALID_REQUEST"))
    }
    case e: Exception => {
      auditHelper.auditIfApiFailure(apiIfFailedAuditRequest, e.getMessage)
      Future.failed(e)
    }
  }

}
