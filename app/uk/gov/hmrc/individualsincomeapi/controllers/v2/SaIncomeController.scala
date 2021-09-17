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

import play.api.hal.Hal.state
import play.api.hal.HalLink
import play.api.libs.json.Json
import play.api.libs.json.Json.obj
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils.{getClientIdHeader, maybeCorrelationId, validateCorrelationId}
import uk.gov.hmrc.individualsincomeapi.services.v2.{SaIncomeService, ScopesHelper, ScopesService}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SaIncomeController @Inject() ( val saIncomeService: SaIncomeService,
                                          val scopeService: ScopesService,
                                          val scopesHelper: ScopesHelper,
                                          val authConnector: AuthConnector,
                                          cc: ControllerComponents,
                                          implicit val auditHelper: AuditHelper)
                                        (implicit val ec: ExecutionContext)
  extends CommonController(cc) with PrivilegedAuthentication {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = super.hc.withExtraHeaders(getClientIdHeader(rh))

  def saFootprint(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("sa"), matchId) { authScopes =>

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchSaFootprint(matchUuid, taxYearInterval, authScopes).map { sa =>

            val correlationId = validateCorrelationId(request)
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val excludeList = Some(List("sa", "paye"))
            val response = Json.toJson(state(saJsObject) ++ scopesHelper.getHalLinks(matchUuid, excludeList, authScopes, None) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa")

  }

  def saReturnsSummary(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("summary"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchSummary(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/summary?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)

            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/summary")
  }

  def saTrustsIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("trusts"), matchId) { authScopes =>

        withValidUuid(matchId) { matchUuid =>

          val correlationId = validateCorrelationId(request)

          saIncomeService.fetchTrusts(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/trusts?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/trusts")
  }

  def saForeignIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("foreign"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

        saIncomeService.fetchForeign(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/foreign?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/foreign")

  }

  def saPartnershipsIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("partnerships"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchPartnerships(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/partnerships?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/partnerships")

  }

  def saInterestsAndDividendsIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("interestsAndDividends"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchInterestAndDividends(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/interests-and-dividends?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/interests-and-dividends")
  }

  def saPensionsAndStateBenefitsIncome(matchId: String,
                                       taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("pensionsAndStateBenefits"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchPensionAndStateBenefits(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/pensions-and-state-benefits")

  }

  def saUkPropertiesIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("ukProperties"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchUkProperties(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/uk-properties?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/uk-properties")

  }

  def saAdditionalInformation(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("additionalInformation"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchAdditionalInformation(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/additional-information?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/additional-information")

  }

  def saOtherIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("other"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchOtherIncome(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/other?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/other")

  }

  def saIncomeSource(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>

        authenticate(scopeService.getEndPointScopes("source"), matchId) { authScopes =>

          val correlationId = validateCorrelationId(request)

          withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchSources(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/source?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }

        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/source")

  }

  def employmentsIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("employments"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchEmployments(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/employments?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/employments")

  }

  def selfEmploymentsIncome(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("selfEmployments"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchSelfEmployments(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/self-employments?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/self-employments")

  }

  def saFurtherDetails(matchId: String, taxYearInterval: TaxYearInterval): Action[AnyContent] = Action.async {
    implicit request =>
      authenticate(scopeService.getEndPointScopes("furtherDetails"), matchId) { authScopes =>

        val correlationId = validateCorrelationId(request)

        withValidUuid(matchId) { matchUuid =>

          saIncomeService.fetchFurtherDetails(matchUuid, taxYearInterval, authScopes).map { sa =>
            val selfLink = HalLink("self", urlWithTaxYearInterval(s"/individuals/income/sa/further-details?matchId=$matchId"))
            val saJsObject = obj("selfAssessment" -> sa)
            val response = Json.toJson(state(saJsObject) ++ selfLink)

            auditHelper.auditSaApiResponse(correlationId.toString, matchId,
              authScopes.mkString(","), request, selfLink.toString, Some(Json.toJson(sa)))

            Ok(response)
          }
        }
      } recover recoveryWithAudit(maybeCorrelationId(request), matchId, "/individuals/income/sa/further-details")

  }
}