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

package uk.gov.hmrc.individualsincomeapi.controllers.v2

import org.joda.time.Interval
import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsincomeapi.domain.v2.Income.incomeJsonFormat
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsincomeapi.services.v2.{IncomeService, LiveIncomeService, SandboxIncomeService, ScopesService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

abstract class IncomeController(incomeService: IncomeService,
                                scopeService: ScopesService,
                                cc: ControllerComponents,
                                implicit  val auditHelper: AuditHelper)
                               (implicit val ec: ExecutionContext)
  extends CommonController(cc) with PrivilegedAuthentication {

  def income(matchId: UUID, interval: Interval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("paye"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        incomeService.fetchIncomeByMatchId(matchId, interval, authScopes).map { paye =>
          val selfLink =
            HalLink("self", urlWithInterval(s"/individuals/income/paye?matchId=$matchId", interval.getStart))

          val incomeJsObject = Json.obj("income" -> toJson(paye))
          val payeJsObject = obj("paye" -> incomeJsObject)
          val response = Json.toJson(state(payeJsObject) ++ selfLink)

          auditHelper.auditApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(paye.map(i => Json.toJson(i))))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/paye")
  }
}

@Singleton
class LiveIncomeController @Inject()(
  val incomeService: LiveIncomeService,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  auditHelper: AuditHelper)
  (override implicit val ec: ExecutionContext)
    extends IncomeController(incomeService, scopeService, cc, auditHelper) {
  override val environment = PRODUCTION
}

@Singleton
class SandboxIncomeController @Inject()(
  val incomeService: SandboxIncomeService,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  auditHelper: AuditHelper)
 (override implicit val ec: ExecutionContext)
    extends IncomeController(incomeService, scopeService, cc, auditHelper) {
  override val environment = SANDBOX
}
