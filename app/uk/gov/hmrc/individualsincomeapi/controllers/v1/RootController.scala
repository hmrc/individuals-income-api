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

package uk.gov.hmrc.individualsincomeapi.controllers.v1

import play.api.hal.Hal._
import play.api.hal.HalLink
import play.api.mvc.hal._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v1.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.services.{CitizenMatchingService, LiveCitizenMatchingService, SandboxCitizenMatchingService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

abstract class RootController(citizenMatchingService: CitizenMatchingService, cc: ControllerComponents)(implicit
  ec: ExecutionContext,
  auditHelper: AuditHelper
) extends CommonController(cc) with PrivilegedAuthentication {

  def root(matchId: UUID): Action[AnyContent] = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income") {
      citizenMatchingService.matchCitizen(matchId) map { _ =>
        val payeLink = HalLink(
          "paye",
          s"/individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
          title = Some("View individual's income per employment")
        )
        val saLink = HalLink(
          "selfAssessment",
          s"/individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
          title = Some("View individual's self-assessment income")
        )
        val selfLink = HalLink("self", s"/individuals/income/?matchId=$matchId")
        Ok(links(saLink, payeLink, selfLink))
      }
    }.recover(recoveryWithAudit(matchId.toString, "/individuals/income/"))
  }
}

@Singleton
class SandboxRootController @Inject() (
  val citizenMatchingService: SandboxCitizenMatchingService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  implicit val auditHelper: AuditHelper
)(implicit ec: ExecutionContext)
    extends RootController(citizenMatchingService, cc) {
  override val environment: String = SANDBOX
}

@Singleton
class LiveRootController @Inject() (
  val citizenMatchingService: LiveCitizenMatchingService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  implicit val auditHelper: AuditHelper
)(implicit ec: ExecutionContext)
    extends RootController(citizenMatchingService, cc) {
  override val environment: String = PRODUCTION
}
