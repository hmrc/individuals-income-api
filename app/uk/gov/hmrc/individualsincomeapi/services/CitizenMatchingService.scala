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

package uk.gov.hmrc.individualsincomeapi.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.individualsincomeapi.connector.IndividualsMatchingApiConnector
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, MatchedCitizen, SandboxIncomeData}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.http.HeaderCarrier

trait CitizenMatchingService {
  def matchCitizen(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen]
}

@Singleton
class SandboxCitizenMatchingService extends CitizenMatchingService {
  override def matchCitizen(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    SandboxIncomeData.matchedCitizen(matchId) match {
      case Some(matchedCitizen) => successful(matchedCitizen)
      case None                 => failed(new MatchNotFoundException)
    }
}

@Singleton
class LiveCitizenMatchingService @Inject()(individualsMatchingApiConnector: IndividualsMatchingApiConnector)
    extends CitizenMatchingService {
  override def matchCitizen(matchId: UUID)(implicit hc: HeaderCarrier): Future[MatchedCitizen] =
    individualsMatchingApiConnector.resolve(matchId)
}
