/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.mvc.hal._
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.Action
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.Environment._
import uk.gov.hmrc.individualsincomeapi.services.{IncomeService, LiveIncomeService, SandboxIncomeService}
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._

import scala.concurrent.ExecutionContext.Implicits.global

abstract class IncomeController(incomeService: IncomeService) extends CommonController with PrivilegedAuthentication {

  def income(matchId: UUID, interval: Interval) = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income-paye") {
      incomeService.fetchIncomeByMatchId(matchId, interval) map { income =>
        val halLink = HalLink("self", urlWithInterval(s"/individuals/income/paye?matchId=$matchId", interval.getStart))
        val incomeJsObject = obj("income" -> toJson(income))
        val payeJsObject = obj("paye" -> incomeJsObject)
        Ok(state(payeJsObject) ++ halLink)
      } recover recovery
    }
  }
}

@Singleton
class LiveIncomeController @Inject()(liveIncomeService: LiveIncomeService, val authConnector: ServiceAuthConnector) extends IncomeController(liveIncomeService) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxIncomeController @Inject()(sandboxIncomeService: SandboxIncomeService, val authConnector: ServiceAuthConnector) extends IncomeController(sandboxIncomeService) {
  override val environment = SANDBOX
}
