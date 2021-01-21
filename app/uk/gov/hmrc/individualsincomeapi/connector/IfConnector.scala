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
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, NotFoundException, TooManyRequestException, Upstream4xxResponse}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPaye, IfPayeEntry, IfSa, IfSaEntry}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class IfConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient, auditHelper: AuditHelper)(
  implicit ec: ExecutionContext) {

  val serviceUrl = servicesConfig.baseUrl("integration-framework")

  lazy val integrationFrameworkBearerToken = servicesConfig.getString(
    "microservice.services.integration-framework.authorization-token"
  )

  lazy val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  private def extractCorrelationId(requestHeader: RequestHeader) =
    requestHeader.headers.get("CorrelationId") match {
      case Some(uuidString) =>
        Try(UUID.fromString(uuidString)) match {
          case Success(_) => uuidString
          case _          => throw new BadRequestException("Malformed CorrelationId")
        }
      case None => throw new BadRequestException("CorrelationId is required")
    }

  private def header(extraHeaders: (String, String)*)(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer $integrationFrameworkBearerToken")))
      .withExtraHeaders(Seq("Environment" -> integrationFrameworkEnvironment) ++ extraHeaders: _*)

  def fetchPayeIncome(nino: Nino, interval: Interval, filter: Option[String])(
    implicit hc: HeaderCarrier,
    request: RequestHeader,
    ec: ExecutionContext): Future[Seq[IfPayeEntry]] = {

    val endpoint = "IfConnector::fetchPayeIncome"

    val startDate = interval.getStart.toLocalDate
    val endDate = interval.getEnd.toLocalDate
    val payeUrl = s"$serviceUrl/individuals/income/paye/" +
      s"nino/$nino?startDate=$startDate&endDate=$endDate${filter.map(f => s"&fields=$f").getOrElse("")}"

    recover[IfPayeEntry](http.GET[IfPaye](payeUrl)(implicitly, header(), ec) map { response =>
      Logger.debug(s"$endpoint - Response: $response")

      auditHelper
        .auditIfApiResponse(
          extractCorrelationId(request),
          None,
          None,
          request,
          payeUrl,
          Json.toJson(response)
        )

      response.paye
    })
  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval, filter: Option[String])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[IfSaEntry]] = {

    val startYear = taxYearInterval.fromTaxYear.endYr
    val endYear = taxYearInterval.toTaxYear.endYr
    val saUrl = s"$serviceUrl/individuals/income/sa/" +
      s"nino/$nino?startYear=$startYear&endYear=$endYear${filter.map(f => s"&fields=$f").getOrElse("")}"

    recover[IfSaEntry](http.GET[IfSa](saUrl)(implicitly, header(), ec).map(_.sa))

  }

  def recover[A](x: Future[Seq[A]]): Future[Seq[A]] = x.recoverWith {
    case _: NotFoundException => {
      // TODO : Audit event here ??
      Future.successful(Seq.empty)
    }
    case Upstream4xxResponse(msg, 429, _, _) => {
      Logger.warn(s"IF Rate limited: $msg")
      // TODO : Audit event here ??
      Future.failed(new TooManyRequestException(msg))
    }
    case e: Exception => {
      // TODO : Audit event here ??
      Future.failed(e)
    }
  }

}
