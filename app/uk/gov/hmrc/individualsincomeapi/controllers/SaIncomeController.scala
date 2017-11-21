/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.json.Json.{obj, toJson}
import play.api.mvc.Action
import play.api.mvc.hal._
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.Environment._
import uk.gov.hmrc.individualsincomeapi.services._
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval

import scala.concurrent.ExecutionContext.Implicits.global

abstract class SaIncomeController(saIncomeService: SaIncomeService) extends CommonController with PrivilegedAuthentication {

  def saFootprint(matchId: UUID, taxYearInterval: TaxYearInterval) = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income-sa") {
      saIncomeService.fetchSaFootprintByMatchId(matchId, taxYearInterval) map { saFootprint =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa?matchId=$matchId"))
        val selfEmploymentsLink = HalLink("selfEmployments", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
        val employmentsLink = HalLink("employments", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
        val summaryLink = HalLink("summary", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
        val selfAssessmentJsObject = obj("selfAssessment" -> toJson(saFootprint))
        Ok(state(selfAssessmentJsObject) ++ selfLink ++ selfEmploymentsLink ++ employmentsLink ++ summaryLink)
      } recover recovery
    }
  }

  def saReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval) = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income-sa-summary") {
      saIncomeService.fetchSaReturnsSummaryByMatchId(matchId, taxYearInterval) map { saReturns =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(saReturns))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      } recover recovery
    }
  }

  def employmentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval) = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income-sa-employments") {
      saIncomeService.fetchEmploymentsIncomeByMatchId(matchId, taxYearInterval) map { employmentsIncome =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(employmentsIncome))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      } recover recovery
    }
  }

  def selfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval) = Action.async { implicit request =>
    requiresPrivilegedAuthentication("read:individuals-income-sa-self-employments") {
      saIncomeService.fetchSelfEmploymentsIncomeByMatchId(matchId, taxYearInterval) map { selfEmploymentsIncome =>
        val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
        val taxReturnsJsObject = obj("taxReturns" -> toJson(selfEmploymentsIncome))
        val selfAssessmentJsObject = obj("selfAssessment" -> taxReturnsJsObject)
        Ok(state(selfAssessmentJsObject) ++ selfLink)
      } recover recovery
    }
  }
}

@Singleton
class SandboxSaIncomeController @Inject()(sandboxSaIncomeService: SandboxSaIncomeService, val authConnector: ServiceAuthConnector) extends SaIncomeController(sandboxSaIncomeService) {
  override val environment = SANDBOX
}

@Singleton
class LiveSaIncomeController @Inject()(liveSaIncomeService: LiveSaIncomeService, val authConnector: ServiceAuthConnector) extends SaIncomeController(liveSaIncomeService) {
  override val environment = PRODUCTION
}
