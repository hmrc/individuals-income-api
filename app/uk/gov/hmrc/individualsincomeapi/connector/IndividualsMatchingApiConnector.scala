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

import java.util.UUID
import javax.inject.Singleton

import uk.gov.hmrc.individualsincomeapi.config.WSHttp
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters.matchedCitizenJsonFormat
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class IndividualsMatchingApiConnector extends ServicesConfig {

  private[connector] val serviceUrl = baseUrl("individuals-matching-api")
  private[connector] val http: HttpGet = WSHttp

  def resolve(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    http.GET[MatchedCitizen](s"$serviceUrl/match-record/$matchId") recover {
      case _: NotFoundException => throw new MatchNotFoundException
    }

}