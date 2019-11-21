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

package uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.joda.time.Interval
import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.hal._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.services.{IncomeService, LiveIncomeService, SandboxIncomeService}

import scala.concurrent.ExecutionContext.Implicits.global

abstract class IncomeController(incomeService: IncomeService) extends CommonController with PrivilegedAuthentication {

  def income(matchId: UUID, interval: Interval): Action[AnyContent] = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income-paye") {
      incomeService.fetchIncomeByMatchId(matchId, interval) map { income =>
        val halLink = HalLink("self", urlWithInterval(s"/individuals/income/paye?matchId=$matchId", interval.getStart))
        val incomeJsObject = obj("income" -> toJson(income))
        val payeJsObject = obj("paye" -> incomeJsObject)
        Ok(state(payeJsObject) ++ halLink)
      }
    }.recover(recovery)
  }
}

@Singleton
class LiveIncomeController @Inject()(val incomeService: LiveIncomeService,
                                     val authConnector: AuthConnector) extends IncomeController(incomeService) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxIncomeController @Inject()(val incomeService: SandboxIncomeService,
                                        val authConnector: AuthConnector) extends IncomeController(incomeService) {
  override val environment = SANDBOX
}