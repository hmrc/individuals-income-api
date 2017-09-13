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

package uk.gov.hmrc.individualsincomeapi.controllers

import javax.inject.{Inject, Singleton}

import play.api.hal.Hal._
import play.api.hal.HalLink
import play.api.mvc.hal._
import play.api.mvc.Action
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.services.{CitizenMatchingService, LiveCitizenMatchingService, SandboxCitizenMatchingService}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class MatchCitizenController(citizenMatchingService: CitizenMatchingService) extends CommonController with PrivilegedAuthentication {
  def matchCitizen(matchId: String) = Action.async { implicit request =>
    requiresPrivilegedAuthentication {
      withUuid(matchId) { matchUuid =>
        citizenMatchingService.matchCitizen(matchUuid) map { _ =>
          val payeLink = HalLink("paye", s"/individuals/income/paye?matchId=$matchId{&fromDate,toDate}", title = Some("View individual's income per employment"))
          val selfLink = HalLink("self", s"/individuals/income/?matchId=$matchId")
          Ok(links(payeLink, selfLink))
        }
      } recover recovery
    }
  }
}

@Singleton
class SandboxMatchCitizenController @Inject()(citizenMatchingService: SandboxCitizenMatchingService, val authConnector: ServiceAuthConnector)
  extends MatchCitizenController(citizenMatchingService) {
  override val environment = SANDBOX
}

@Singleton
class LiveMatchCitizenController @Inject()(citizenMatchingService: LiveCitizenMatchingService, val authConnector: ServiceAuthConnector)
  extends MatchCitizenController(citizenMatchingService) {
  override val environment = PRODUCTION
}
