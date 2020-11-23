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
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.getClientIdHeader
import uk.gov.hmrc.individualsincomeapi.services._

import scala.concurrent.ExecutionContext.Implicits.global

sealed abstract class SaIncomeController(
  saIncomeService: SaIncomeService,
  scopeService: ScopesService,
  cc: ControllerComponents)
    extends CommonController(cc) with PrivilegedAuthentication {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc.withExtraHeaders(getClientIdHeader(rh))

  def saFootprint(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSa")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaSummary")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saTrustsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaTrusts")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saForeignIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaForeign")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saPartnershipsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaPartnerships")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saInterestsAndDividendsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] =
    Action.async { implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaInterestsAndDividends")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
    }

  def saPensionsAndStateBenefitsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] =
    Action.async { implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaPensionsAndStateBenefits")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
    }

  def saUkPropertiesIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaUkProperties")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaAdditionalInformation")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaOther")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saIncomeSource(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaSource")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def employmentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaEmployments")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def selfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaSelfEmployments")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }

  def saFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      {
        val scopes = scopeService.getEndPointScopes("incomeSaFurtherDetails")
        requiresPrivilegedAuthentication(scopes)
          .flatMap { authScopes =>
            throw new Exception("NOT_IMPLEMENTED")
          }
          .recover(recovery)
      }
  }
}

@Singleton
class SandboxSaIncomeController @Inject()(
  val saIncomeService: SandboxSaIncomeService,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends SaIncomeController(saIncomeService, scopeService, cc) {
  override val environment = SANDBOX
}

@Singleton
class LiveSaIncomeController @Inject()(
  val saIncomeService: LiveSaIncomeService,
  val scopeService: ScopesService,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends SaIncomeController(saIncomeService, scopeService, cc) {
  override val environment = PRODUCTION
}
