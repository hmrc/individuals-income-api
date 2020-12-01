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

package uk.gov.hmrc.individualsincomeapi.controllers.v2

import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.joda.time.Interval
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsincomeapi.services.v2.{IncomeServiceV2, LiveIncomeServiceV2, SandboxIncomeServiceV2}
import uk.gov.hmrc.individualsincomeapi.services.v2.ScopesService

import scala.concurrent.ExecutionContext.Implicits.global

abstract class IncomeController(incomeService: IncomeServiceV2, scopeService: ScopesService, cc: ControllerComponents)
    extends CommonController(cc) with PrivilegedAuthentication {

  def income(matchId: UUID, interval: Interval): Action[AnyContent] = Action.async { implicit request =>
    {
      val scopes = scopeService.getEndPointScopes("incomePaye")
      requiresPrivilegedAuthentication(scopes)
        .flatMap { authScopes =>
          throw new Exception("NOT_IMPLEMENTED")
        }
        .recover(recovery)
    }
  }
}

@Singleton
class LiveIncomeController @Inject()(
  val incomeService: LiveIncomeServiceV2,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends IncomeController(incomeService, scopeService, cc) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxIncomeController @Inject()(
  val incomeService: SandboxIncomeServiceV2,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends IncomeController(incomeService, scopeService, cc) {
  override val environment = SANDBOX
}
