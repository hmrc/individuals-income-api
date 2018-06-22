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
import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.hal._
import play.api.mvc.{Action, AnyContent, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.actions.{LivePrivilegedAction, PrivilegedAction, SandboxPrivilegedAction}
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.getClientIdHeader
import uk.gov.hmrc.individualsincomeapi.services._

import scala.concurrent.ExecutionContext.Implicits.global

sealed trait SaIncomeController extends CommonController {
  val saIncomeService: SaIncomeService
  val privilegedAction: PrivilegedAction

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc.withExtraHeaders(getClientIdHeader(rh))

  def saFootprint(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa") { implicit request =>
      saIncomeService.fetchSaFootprint(matchId, taxYearInterval) map { saFootprint =>
        Ok(state(obj("selfAssessment" -> toJson(saFootprint)))
          ++ HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa?matchId=$matchId"))
          ++ HalLink("additionalInformation", urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId"))
          ++ HalLink("employments", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
          ++ HalLink("foreign", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))
          ++ HalLink("interestsAndDividends", urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId"))
          ++ HalLink("other", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))
          ++ HalLink("partnerships", urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId"))
          ++ HalLink("pensionsAndStateBenefits", urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId"))
          ++ HalLink("selfEmployments", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
          ++ HalLink("summary", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
          ++ HalLink("trusts", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))
          ++ HalLink("ukProperties", urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId")))
      }
    }
  }

  def saReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-summary") { implicit request =>
      saIncomeService.fetchReturnsSummary(matchId, taxYearInterval) map { saReturns =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saReturns))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saTrustsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-trusts") { implicit request =>
      saIncomeService.fetchTrustsIncome(matchId, taxYearInterval) map { saTrusts =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saTrusts))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saForeignIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-foreign") { implicit request =>
      saIncomeService.fetchForeignIncome(matchId, taxYearInterval) map { saForeignIncomes =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saForeignIncomes))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saPartnershipsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-partnerships") { implicit request =>
      saIncomeService.fetchPartnershipsIncome(matchId, taxYearInterval) map { saPartnerships =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saPartnerships))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saInterestsAndDividendsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-interests-and-dividends") { implicit request =>
      saIncomeService.fetchInterestsAndDividendsIncome(matchId, taxYearInterval) map { saInterestsAndDividends =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saInterestsAndDividends))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saPensionsAndStateBenefitsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-pensions-and-state-benefits") { implicit request =>
      saIncomeService.fetchPensionsAndStateBenefitsIncome(matchId, taxYearInterval) map { saPensionsAndStateBenefits =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saPensionsAndStateBenefits))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saUkPropertiesIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-uk-properties") { implicit request =>
      saIncomeService.fetchUkPropertiesIncome(matchId, taxYearInterval) map { saUkPropertiesIncomes =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saUkPropertiesIncomes))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-additional-information") { implicit request =>
      saIncomeService.fetchAdditionalInformation(matchId, taxYearInterval) map { saAdditionalInformation =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saAdditionalInformation))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-other") { implicit request =>
      saIncomeService.fetchOtherIncome(matchId, taxYearInterval) map { saOtherIncome =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saOtherIncome))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def saTradeDescription(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] ={
    privilegedAction("read:individuals-income-sa-trade-description") { implicit request =>
      saIncomeService.fetchSaTradeDescription(matchId, taxYearInterval) map { tradeDescription =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trade-description?matchId=$matchId"))
        val json = Json.obj("selfAssessment" -> Json.obj("taxReturns" -> Json.toJson(tradeDescription)))
        Ok(state(json) ++ selfLink)
      }
    }
  }

  def saTradingAddress(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-trading-address") { implicit request =>
      saIncomeService.fetchSaTradingAddress(matchId, taxYearInterval) map { tradingAddresses =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trading-address?matchId=$matchId"))
        val json = Json.obj("selfAssessment" -> Json.obj("taxReturns" -> Json.toJson(tradingAddresses)))
        Ok(state(json) ++ selfLink)
      }
    }
  }

  def employmentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-employments") { implicit request =>
      saIncomeService.fetchEmploymentsIncome(matchId, taxYearInterval) map { employmentsIncome =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(employmentsIncome))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }

  def selfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = {
    privilegedAction("read:individuals-income-sa-self-employments") { implicit request =>
      saIncomeService.fetchSelfEmploymentsIncome(matchId, taxYearInterval) map { selfEmploymentsIncome =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(selfEmploymentsIncome))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      }
    }
  }
}

@Singleton
class SandboxSaIncomeController @Inject()(val saIncomeService: SandboxSaIncomeService,
                                          val privilegedAction: SandboxPrivilegedAction) extends SaIncomeController

@Singleton
class LiveSaIncomeController @Inject()(val saIncomeService: LiveSaIncomeService,
                                       val privilegedAction: LivePrivilegedAction) extends SaIncomeController