/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.Configuration
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpReads, NotFoundException}
import uk.gov.hmrc.individualsincomeapi.config.WSHttp
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.CLIENT_ID_HEADER
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(configuration: Configuration) extends ServicesConfig {

  val serviceUrl = baseUrl("des")
  val http: HttpGet = WSHttp
  val desBearerToken = configuration.getString("microservice.services.des.authorization-token").getOrElse(throw new RuntimeException("DES authorization token must be defined"))
  val desEnvironment = configuration.getString("microservice.services.des.environment").getOrElse(throw new RuntimeException("DES environment must be defined"))

  private def header(extraHeaders: (String, String)*)(implicit hc: HeaderCarrier) = {
    hc.copy(authorization = Some(Authorization(s"Bearer $desBearerToken")))
      .withExtraHeaders(Seq("Environment" -> desEnvironment, "Source" -> "MDTP") ++ extraHeaders: _*)
  }

  def fetchEmployments(nino: Nino, interval: Interval)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[DesEmployment]] = {
    val fromDate = interval.getStart.toLocalDate
    val toDate = interval.getEnd.toLocalDate

    http.GET[DesEmployments](s"$serviceUrl/individuals/nino/$nino/employments/income?from=$fromDate&to=$toDate")(
      implicitly[HttpReads[DesEmployments]], header(), ec) map (_.employments) recover {
      case _: NotFoundException => Seq.empty
    }
  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[DesSAIncome]] = {
    val fromTaxYear = taxYearInterval.fromTaxYear.endYr
    val toTaxYear = taxYearInterval.toTaxYear.endYr
    val originator = hc.headers.toMap.get(CLIENT_ID_HEADER).map(id => s"MDTP_CLIENTID=$id").getOrElse("-")

    http.GET[Seq[DesSAIncome]](s"$serviceUrl/individuals/nino/$nino/self-assessment/income?startYear=$fromTaxYear&endYear=$toTaxYear")(
      implicitly[HttpReads[Seq[DesSAIncome]]], header("OriginatorId" -> originator), ec) recover {
      case _: NotFoundException => Seq.empty
    }
  }

}
