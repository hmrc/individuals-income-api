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
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.{PRODUCTION, SANDBOX}
import uk.gov.hmrc.individualsincomeapi.controllers.{CommonController, PrivilegedAuthentication}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.{getClientIdHeader, maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveSaIncomeService, SaIncomeService, SandboxSaIncomeService, ScopesHelper, ScopesService}

import scala.concurrent.ExecutionContext

sealed abstract class SaIncomeController(saIncomeService: SaIncomeService,
                                         scopeService: ScopesService,
                                         scopesHelper: ScopesHelper,
                                         cc: ControllerComponents,
                                         implicit val auditHelper: AuditHelper)
                                        (implicit val ec: ExecutionContext)
  extends CommonController(cc) with PrivilegedAuthentication {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc.withExtraHeaders(getClientIdHeader(rh))

  def saFootprint(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("sa"), matchId.toString) { authScopes =>
        saIncomeService.fetchSaFootprint(matchId, taxYearInterval, authScopes).map { sa =>

          val correlationId = validateCorrelationId(request)
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val excludeList = Some(List("sa", "paye"))
          val response = Json.toJson(state(saJsObject) ++ scopesHelper.getHalLinks(matchId, excludeList, authScopes, None) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa")

  }

  def saReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("summary"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchSummary(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)

          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/summary")
  }

  def saTrustsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("trusts"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchTrusts(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper.auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/trusts")
  }

  def saForeignIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("foreign"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchForeign(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/foreign")

  }

  def saPartnershipsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("partnerships"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchPartnerships(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/partnerships")

  }

  def saInterestsAndDividendsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("interestsAndDividends"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchInterestAndDividends(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
          }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/interests-and-dividends")
  }

  def saPensionsAndStateBenefitsIncome(matchId: UUID,
                                       taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("pensionsAndStateBenefits"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request);

        saIncomeService.fetchPensionAndStateBenefits(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/pensions-and-state-benefits")

  }

  def saUkPropertiesIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("ukProperties"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchUkProperties(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/uk-properties")

  }

  def saAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("additionalInformation"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchAdditionalInformation(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
          }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/additional-information")

  }

  def saOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("other"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchOtherIncome(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/other")

  }

  def saIncomeSource(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>

      val correlationId = validateCorrelationId(request)

      authenticate(scopeService.getEndPointScopes("source"), matchId.toString) { authScopes =>
        saIncomeService.fetchSources(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/source?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/source")

  }

  def employmentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("employments"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchEmployments(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/employments")

  }

  def selfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("selfEmployments"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchSelfEmployments(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/self-employments")

  }

  def saFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("furtherDetails"), matchId.toString) { authScopes =>

        val correlationId = validateCorrelationId(request)

        saIncomeService.fetchFurtherDetails(matchId, taxYearInterval, authScopes).map { sa =>
          val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/further-details?matchId=$matchId"))
          val saJsObject = obj("selfAssessment" -> sa)
          val response = Json.toJson(state(saJsObject) ++ selfLink)

          auditHelper. auditSaApiResponse(correlationId.toString, matchId.toString,
            authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

          Ok(response)
          }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId.toString, "/individuals/income/sa/further-details")

  }
}

@Singleton
class SandboxSaIncomeController @Inject()(
  val saIncomeService: SandboxSaIncomeService,
  val scopeService: ScopesService,
  val scopesHelper: ScopesHelper,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  auditHelper: AuditHelper)
  (implicit override val ec: ExecutionContext)
    extends SaIncomeController(saIncomeService, scopeService, scopesHelper, cc, auditHelper) {
  override val environment = SANDBOX
}

@Singleton
class LiveSaIncomeController @Inject()(
  val saIncomeService: LiveSaIncomeService,
  val scopeService: ScopesService,
  val scopesHelper: ScopesHelper,
  val authConnector: AuthConnector,
  cc: ControllerComponents,
  auditHelper: AuditHelper)
  (implicit override val ec: ExecutionContext)
    extends SaIncomeController(saIncomeService, scopeService, scopesHelper, cc, auditHelper) {
  override val environment = PRODUCTION
}
