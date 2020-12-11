/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, NotFoundException, TooManyRequestException, Upstream4xxResponse}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.{IfPaye, IfPayeEntry}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa.{IfSa, IfSaEntry}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IfConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  val serviceUrl = servicesConfig.baseUrl("integration-framework")

  lazy val integrationFrameworkBearerToken = servicesConfig.getString(
    "microservice.services.integration-framework.authorization-token"
  )

  lazy val integrationFrameworkEnvironment = servicesConfig.getString(
    "microservice.services.integration-framework.environment"
  )

  private def header(extraHeaders: (String, String)*)(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer $integrationFrameworkBearerToken")))
      .withExtraHeaders(Seq("Environment" -> integrationFrameworkEnvironment) ++ extraHeaders: _*)

  def fetchPayeIncome(nino: Nino, interval: Interval, filter: Option[String])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[IfPayeEntry]] = {

    val startDate = interval.getStart.toLocalDate
    val endDate = interval.getEnd.toLocalDate
    val payeUrl = s"$serviceUrl/individuals/income/paye/" +
      s"nino/$nino?startDate=$startDate&endDate=$endDate&fields=${filter.map(f => s"&fields=$f").getOrElse("")}"

    recover[IfPayeEntry](http.GET[IfPaye](payeUrl)(implicitly, header(), ec).map(_.paye))
  }

  def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval, filter: Option[String])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Seq[IfSaEntry]] = {

    val startYear = taxYearInterval.fromTaxYear.endYr
    val endYear = taxYearInterval.toTaxYear.endYr
    val saUrl = s"$serviceUrl/individuals/income/sa/" +
      s"nino/$nino?startYear=$startYear&endYear=$endYear&fields=$filter"

    recover[IfSaEntry](http.GET[IfSa](saUrl)(implicitly, header(), ec).map(_.sa))

  }

  def recover[A](x: Future[Seq[A]]): Future[Seq[A]] = x.recoverWith {
    case _: NotFoundException => Future.successful(Seq.empty)
    case Upstream4xxResponse(msg, 429, _, _) => {
      Logger.warn(s"IF Rate limited: $msg")
      Future.failed(new TooManyRequestException(msg))
    }
    case e: Exception => {
      Logger.warn(s"IfConnector:Exception: ${e.getMessage}")
      Future.failed(e)
    }
  }

}
