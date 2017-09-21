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
import uk.gov.hmrc.individualsincomeapi.config.WSHttp
import uk.gov.hmrc.individualsincomeapi.domain.{DesEmployment, DesEmployments}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpReads, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._

@Singleton
class DesConnector @Inject()(configuration: Configuration) extends ServicesConfig {

  val serviceUrl = baseUrl("des")
  val http: HttpGet = WSHttp
  val desBearerToken = configuration.getString("microservice.services.des.authorization-token").getOrElse(throw new RuntimeException("DES authorization token must be defined"))
  val desEnvironment = configuration.getString("microservice.services.des.environment").getOrElse(throw new RuntimeException("DES environment must be defined"))

  def fetchEmployments(nino: Nino, interval: Interval)(implicit hc: HeaderCarrier): Future[Seq[DesEmployment]] = {
    val fromDate = interval.getStart.toLocalDate
    val toDate = interval.getEnd.toLocalDate
    val header = hc.copy(authorization = Some(Authorization(s"Bearer $desBearerToken"))).withExtraHeaders("Environment" -> desEnvironment, "Source" -> "MDTP")

    http.GET[DesEmployments](s"$serviceUrl/individuals/nino/$nino/employments/income?from=$fromDate&to=$toDate")(
      implicitly[HttpReads[DesEmployments]], header) map (_.employments) recover {
      case _: NotFoundException => Seq.empty
    }
  }
}
