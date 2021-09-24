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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, Upstream4xxResponse}
import uk.gov.hmrc.individualsincomeapi.domain.v1.JsonFormatters.matchedCitizenJsonFormat
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IndividualsMatchingApiConnector @Inject()(servicesConfig: ServicesConfig, http: HttpClient) {

  private[connector] val serviceUrl = servicesConfig.baseUrl("individuals-matching-api")

  def resolve(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    http.GET[MatchedCitizen](s"$serviceUrl/match-record/$matchId") recover {
      case Upstream4xxResponse(_, 404, _, _) => throw new MatchNotFoundException
    }

}
