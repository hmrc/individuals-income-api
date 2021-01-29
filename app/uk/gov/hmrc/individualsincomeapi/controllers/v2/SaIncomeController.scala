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

import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.{extractCorrelationId, getClientIdHeader}
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveSaIncomeService, SaIncomeService, SandboxSaIncomeService, ScopesHelper, ScopesService}

import scala.concurrent.ExecutionContext.Implicits.global

sealed abstract class SaIncomeController(
  saIncomeService: SaIncomeService,
  scopeService: ScopesService,
  scopesHelper: ScopesHelper,
  cc: ControllerComponents)
    extends CommonController(cc) with PrivilegedAuthentication {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc.withExtraHeaders(getClientIdHeader(rh))

  def saFootprint(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("sa")) { authScopes =>
        saIncomeService.fetchSaFootprint(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          val excludeList = Some(List("sa", "paye"))

          Ok(Json.toJson(
            state(saJsObject) ++ scopesHelper.getHalLinks(matchId, excludeList, authScopes, None) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("summary")) { authScopes =>
        saIncomeService.fetchSummary(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)
  }

  def saTrustsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("trusts")) { authScopes =>
        saIncomeService.fetchTrusts(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)
  }

  def saForeignIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("foreign")) { authScopes =>
        saIncomeService.fetchForeign(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saPartnershipsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("partnerships")) { authScopes =>
        saIncomeService.fetchPartnerships(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saInterestsAndDividendsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] =
    Action.async { implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("interestsAndDividends")) { authScopes =>
        saIncomeService.fetchInterestAndDividends(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)
    }

  def saPensionsAndStateBenefitsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] =
    Action.async { implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("pensionsAndStateBenefits")) { authScopes =>
        saIncomeService.fetchPensionAndStateBenefits(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink(
              "self",
              urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

    }

  def saUkPropertiesIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("ukProperties")) { authScopes =>
        saIncomeService.fetchUkProperties(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("additionalInformation")) { authScopes =>
        saIncomeService.fetchAdditionalInformation(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("other")) { authScopes =>
        saIncomeService.fetchOtherIncome(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saIncomeSource(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("source")) { authScopes =>
        saIncomeService.fetchSources(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/source?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def employmentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("employments")) { authScopes =>
        saIncomeService.fetchEmployments(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def selfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("employments")) { authScopes =>
        saIncomeService.fetchSelfEmployments(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }

  def saFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      extractCorrelationId(request)
      requiresPrivilegedAuthentication(scopeService.getEndPointScopes("furtherDetails")) { authScopes =>
        saIncomeService.fetchFurtherDetails(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink =
            HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/further-details?matchId=$matchId"))

          val saJsObject = obj("selfAssessment" -> sa)

          Ok(Json.toJson(state(saJsObject) ++ selfLink))
        }
      }.recover(recovery)

  }
}

@Singleton
class SandboxSaIncomeController @Inject()(
  val saIncomeService: SandboxSaIncomeService,
  val scopeService: ScopesService,
  val scopesHelper: ScopesHelper,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends SaIncomeController(saIncomeService, scopeService, scopesHelper, cc) {
  override val environment = SANDBOX
}

@Singleton
class LiveSaIncomeController @Inject()(
  val saIncomeService: LiveSaIncomeService,
  val scopeService: ScopesService,
  val scopesHelper: ScopesHelper,
  val authConnector: AuthConnector,
  cc: ControllerComponents)
    extends SaIncomeController(saIncomeService, scopeService, scopesHelper, cc) {
  override val environment = PRODUCTION
}
