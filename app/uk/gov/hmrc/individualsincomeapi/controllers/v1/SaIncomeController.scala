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

import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.hal._
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v1.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.domain.v1.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.getClientIdHeader
import uk.gov.hmrc.individualsincomeapi.services.v1.{LiveSaIncomeService, SaIncomeService, SandboxSaIncomeService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

sealed abstract class SaIncomeController(saIncomeService: SaIncomeService, cc: ControllerComponents)(implicit
  ec: ExecutionContext,
  auditHelper: AuditHelper
) extends CommonController(cc) with PrivilegedAuthentication {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc.withExtraHeaders(getClientIdHeader(rh))

  def saFootprint(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      requiresPrivilegedAuthentication("read:individuals-income-sa") {
        saIncomeService.fetchSaFootprint(matchId, taxYearInterval) map { saFootprint =>
          Ok(
            state(obj("selfAssessment" -> toJson(saFootprint)))
              ++ HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa?matchId=$matchId"))
              ++ HalLink(
                "additionalInformation",
                urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId")
              )
              ++ HalLink("employments", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
              ++ HalLink("foreign", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))
              ++ HalLink(
                "interestsAndDividends",
                urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId")
              )
              ++ HalLink("other", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))
              ++ HalLink(
                "partnerships",
                urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId")
              )
              ++ HalLink(
                "pensionsAndStateBenefits",
                urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId")
              )
              ++ HalLink(
                "selfEmployments",
                urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId")
              )
              ++ HalLink("summary", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
              ++ HalLink("trusts", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))
              ++ HalLink(
                "ukProperties",
                urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId")
              )
          )
        }
      }.recover(recoveryWithAudit(matchId.toString, "/individuals/income/sa/"))
  }

  def saReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-summary") {
        saIncomeService.fetchReturnsSummary(matchId, taxYearInterval) map { saReturns =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saReturns))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saTrustsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-trusts") {
        saIncomeService.fetchTrustsIncome(matchId, taxYearInterval) map { saTrusts =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saTrusts))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saForeignIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-foreign") {
        saIncomeService.fetchForeignIncome(matchId, taxYearInterval) map { saForeignIncomes =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saForeignIncomes))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saPartnershipsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink =
        HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-partnerships") {
        saIncomeService.fetchPartnershipsIncome(matchId, taxYearInterval) map { saPartnerships =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saPartnerships))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saInterestsAndDividendsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] =
    Action.async { implicit request =>
      val selfLink =
        HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-interests-and-dividends") {
        saIncomeService.fetchInterestsAndDividendsIncome(matchId, taxYearInterval) map { saInterestsAndDividends =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saInterestsAndDividends))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
    }

  def saPensionsAndStateBenefitsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] =
    Action.async { implicit request =>
      val selfLink = HalLink(
        "self",
        urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId")
      )
      requiresPrivilegedAuthentication("read:individuals-income-sa-pensions-and-state-benefits") {

        saIncomeService.fetchPensionsAndStateBenefitsIncome(matchId, taxYearInterval) map {
          saPensionsAndStateBenefits =>
            val taxReturnsJsObject = obj("taxReturns" -> toJson(saPensionsAndStateBenefits))
            val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
            Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
    }

  def saUkPropertiesIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink =
        HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-uk-properties") {
        saIncomeService.fetchUkPropertiesIncome(matchId, taxYearInterval) map { saUkPropertiesIncomes =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saUkPropertiesIncomes))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink =
        HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-additional-information") {
        saIncomeService.fetchAdditionalInformation(matchId, taxYearInterval) map { saAdditionalInformation =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saAdditionalInformation))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-other") {
        saIncomeService.fetchOtherIncome(matchId, taxYearInterval) map { saOtherIncome =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(saOtherIncome))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def saIncomeSource(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/sources?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-source") {
        saIncomeService.fetchSaIncomeSources(matchId, taxYearInterval) map { sources =>
          val json = Json.obj("selfAssessment" -> Json.obj("taxReturns" -> Json.toJson(sources)))
          Ok(state(json) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def employmentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-employments") {
        saIncomeService.fetchEmploymentsIncome(matchId, taxYearInterval) map { employmentsIncome =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(employmentsIncome))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }

  def selfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      val selfLink =
        HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
      requiresPrivilegedAuthentication("read:individuals-income-sa-self-employments") {
        saIncomeService.fetchSelfEmploymentsIncome(matchId, taxYearInterval) map { selfEmploymentsIncome =>
          val taxReturnsJsObject = obj("taxReturns" -> toJson(selfEmploymentsIncome))
          val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
          Ok(state(selfAssessmentJsObject) ++ selfLink)
        }
      }.recover(recoveryWithAudit(matchId.toString, selfLink.href))
  }
}

@Singleton
class SandboxSaIncomeController @Inject() (
  val saIncomeService: SandboxSaIncomeService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  implicit val auditHelper: AuditHelper
)(implicit ec: ExecutionContext)
    extends SaIncomeController(saIncomeService, cc) {
  override val environment: String = SANDBOX
}

@Singleton
class LiveSaIncomeController @Inject() (
  val saIncomeService: LiveSaIncomeService,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  implicit val auditHelper: AuditHelper
)(implicit ec: ExecutionContext)
    extends SaIncomeController(saIncomeService, cc) {
  override val environment: String = PRODUCTION
}
