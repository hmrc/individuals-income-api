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

import javax.inject.{Inject, Singleton}
import org.joda.time.Interval
import play.api.Logger
import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.individualsincomeapi.domain.v1.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesEmployment, DesEmployments, DesSAIncome}
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.CLIENT_ID_HEADER
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  val logger: Logger = Logger(getClass)

  val serviceUrl = servicesConfig.baseUrl("des")

  lazy val desBearerToken = servicesConfig.getString("microservice.services.des.authorization-token")
  lazy val desEnvironment = servicesConfig.getString("microservice.services.des.environment")

  def setHeaders = Seq(
    HeaderNames.authorisation -> s"Bearer $desBearerToken",
    "Environment"             -> desEnvironment,
    "Source"                  -> "MDTP"
  )

  def fetchEmployments(nino: Nino, interval: Interval)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[DesEmployment]] = {
    val fromDate = interval.getStart.toLocalDate
    val toDate = interval.getEnd.toLocalDate

    val employmentsUrl = s"$serviceUrl/individuals/nino/$nino/employments/income?from=$fromDate&to=$toDate"

    recover[DesEmployment](http.GET[DesEmployments](employmentsUrl, headers = setHeaders).map(_.employments))
  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[DesSAIncome]] = {

    val fromTaxYear = taxYearInterval.fromTaxYear.endYr
    val toTaxYear   = taxYearInterval.toTaxYear.endYr
    val originator  = hc.extraHeaders.toMap.get(CLIENT_ID_HEADER).map(id => s"MDTP_CLIENTID=$id").getOrElse("-")

    implicit val saIncomeReads: Reads[DesSAIncome] = DesSAIncome.desReads

    val saIncomeUrl =
      s"$serviceUrl/individuals/nino/$nino/self-assessment/income?startYear=$fromTaxYear&endYear=$toTaxYear"

    recover[DesSAIncome](http.GET[Seq[DesSAIncome]](saIncomeUrl, headers = setHeaders :+ ("OriginatorId" -> originator)))
  }

  def recover[A](x: Future[Seq[A]]): Future[Seq[A]] = x.recoverWith {
    case _: NotFoundException => Future.successful(Seq.empty)
    case Upstream4xxResponse(msg, 429, _, _) => {
      logger.warn(s"DES Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    }
  }

}
