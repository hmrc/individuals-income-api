/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.{maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsincomeapi.services.LiveCitizenMatchingService
import uk.gov.hmrc.individualsincomeapi.services.v2.{ScopesHelper, ScopesService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RootController @Inject()(
                                val citizenMatchingService: LiveCitizenMatchingService,
                                val scopeService: ScopesService,
                                scopesHelper: ScopesHelper,
                                val authConnector: AuthConnector,
                                implicit val auditHelper: AuditHelper,
                                cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends CommonController(cc) with PrivilegedAuthentication {

  def root(matchId: String): Action[AnyContent] = Action.async { implicit request =>
    authenticate(scopeService.getAllScopes, matchId.toString) { authScopes =>

      withValidUuid(matchId, "matchId format is invalid") { matchUuid =>

        val correlationId = validateCorrelationId(request);

        citizenMatchingService.matchCitizen(matchUuid) map { _: MatchedCitizen =>
          val selfLink = HalLink("self", s"/individuals/income/?matchId=$matchId")
          val allowedList = Some(List("sa", "paye"))
          val excludeList = Some(List())

          val response = Json.toJson(scopesHelper.getHalLinks(matchUuid, excludeList, authScopes, allowedList) ++ selfLink)

          auditHelper.auditApiResponse(correlationId.toString, matchId,
            authScopes.mkString(","), request, response.toString, None)

          Ok(response)
        }
      }
    }recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income")
  }
}