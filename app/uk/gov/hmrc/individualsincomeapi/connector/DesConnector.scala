/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import org.joda.time.Interval
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.CLIENT_ID_HEADER
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient) {

  val serviceUrl = servicesConfig.baseUrl("des")

  lazy val desBearerToken = servicesConfig.getString("microservice.services.des.authorization-token")
  lazy val desEnvironment = servicesConfig.getString("microservice.services.des.environment")

  private def header(extraHeaders: (String, String)*)(implicit hc: HeaderCarrier) = {
    hc.copy(authorization = Some(Authorization(s"Bearer $desBearerToken")))
      .withExtraHeaders(Seq("Environment" -> desEnvironment, "Source" -> "MDTP") ++ extraHeaders: _*)
  }

  def fetchEmployments(nino: Nino, interval: Interval)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[DesEmployment]] = {
    val fromDate = interval.getStart.toLocalDate
    val toDate = interval.getEnd.toLocalDate

    val employmentsUrl = s"$serviceUrl/individuals/nino/$nino/employments/income?from=$fromDate&to=$toDate"
    http.GET[DesEmployments](employmentsUrl)(implicitly, header(), ec).map(_.employments).recoverWith {
      case _: NotFoundException => Future.successful(Seq.empty)
      case Upstream5xxResponse(msg, 503, _) if msg.contains("LTM000503") /* DES's magic rate limit error code*/ =>
        Future.failed(new TooManyRequestException(msg))
    }
  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[DesSAIncome]] = {
    val fromTaxYear = taxYearInterval.fromTaxYear.endYr
    val toTaxYear = taxYearInterval.toTaxYear.endYr
    val originator = hc.headers.toMap.get(CLIENT_ID_HEADER).map(id => s"MDTP_CLIENTID=$id").getOrElse("-")
    implicit val saIncomeReads: Reads[DesSAIncome] = DesSAIncome.desReads

    val saIncomeUrl = s"$serviceUrl/individuals/nino/$nino/self-assessment/income?startYear=$fromTaxYear&endYear=$toTaxYear"

    http.GET[Seq[DesSAIncome]](saIncomeUrl)(implicitly, header("OriginatorId" -> originator), ec).recoverWith {
      case _: NotFoundException => Future.successful(Seq.empty)
      case Upstream5xxResponse(msg, 503, _) if msg.contains("LTM000503") /* DES's magic rate limit error code*/ =>
        Future.failed(new TooManyRequestException(msg))
    }
  }

}
