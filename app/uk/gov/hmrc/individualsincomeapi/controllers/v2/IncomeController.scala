/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain.v2.Income.incomeJsonFormat
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsincomeapi.services.v2.{IncomeService, ScopesService}
import uk.gov.hmrc.individualsincomeapi.util.Interval

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IncomeController @Inject() (
  val incomeService: IncomeService,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  implicit val auditHelper: AuditHelper
)(implicit val ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def income(matchId: String, interval: Interval): Action[AnyContent] = Action.async { implicit request =>
    authenticate(scopeService.getEndPointScopes("paye"), matchId.toString) { authScopes =>
      val correlationId = validateCorrelationId(request)

      withValidUuid(matchId, "matchId format is invalid") { matchUuid =>
        incomeService.fetchIncomeByMatchId(matchUuid, interval, authScopes).map { paye =>
          val selfLink =
            HalLink("self", urlWithInterval(s"/individuals/income/paye?matchId=$matchId", interval.fromDate))

          val incomeJsObject = Json.obj("income" -> toJson(paye))
          val payeJsObject = obj("paye" -> incomeJsObject)
          val response = Json.toJson(state(payeJsObject) ++ selfLink)

          auditHelper.auditApiResponse(
            correlationId.toString,
            matchId.toString,
            authScopes.mkString(","),
            request,
            selfLink.toString,
            Some(paye.map(i => Json.toJson(i)))
          )

          Ok(response)
        }
      }
    } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/paye")
  }
}
