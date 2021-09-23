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

package uk.gov.hmrc.individualsincomeapi.services.v2

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2._

import java.util.UUID
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SaIncomeService @Inject()(matchingConnector: IndividualsMatchingApiConnector,
                                ifConnector: IfConnector,
                                cache: CacheService,
                                scopeService: ScopesService,
                                scopesHelper: ScopesHelper,
                                @Named("retryDelay") retryDelay: Int)
                               (implicit ec: ExecutionContext) {

  def endpoints =
    List(
      "sa",
      "summary",
      "trusts",
      "foreign",
      "partnerships",
      "interestsAndDividends",
      "pensionsAndStateBenefits",
      "ukProperties",
      "additionalInformation",
      "other",
      "source",
      "employments",
      "selfEmployments",
      "furtherDetails"
    )

  private def fetchSaIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[Seq[IfSaEntry]] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      saIncome <- cache.get(
        SaCacheId(
          matchId,
          taxYearInterval,
          scopeService.getValidFieldsForCacheKey(scopes.toList, endpoints)),
        withRetry(
          ifConnector.fetchSelfAssessmentIncome(
            ninoMatch.nino,
            taxYearInterval,
            Option(scopesHelper.getQueryStringFor(scopes.toList, endpoints)).filter(_.nonEmpty),
            matchId.toString
          )
        )
      )
    } yield saIncome

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case Upstream5xxResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }

  def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFootprint] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFootprint.transform(ifSaEntries)

  def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSummaries] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSummaries.transform(ifSaEntries)

  def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                          (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaTrusts] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaTrusts.transform(ifSaEntries)

  def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaForeignIncomes] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaForeignIncomes.transform(ifSaEntries)

  def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaPartnerships] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaPartnerships.transform(ifSaEntries)

  def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                        (implicit hc: HeaderCarrier,
                                         request: RequestHeader): Future[SaInterestAndDividends] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaInterestAndDividends.transform(ifSaEntries)

  def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                           (implicit hc: HeaderCarrier,
                                            request: RequestHeader): Future[SaPensionAndStateBenefits] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaPensionAndStateBenefits.transform(ifSaEntries)

  def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaUkProperties] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaUkProperties.transform(ifSaEntries)

  def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                         (implicit hc: HeaderCarrier,
                                          request: RequestHeader): Future[SaAdditionalInformationRecords] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaAdditionalInformationRecords.transform(ifSaEntries)

  def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaOtherIncomeRecords] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaOtherIncomeRecords.transform(ifSaEntries)

  def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSources] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSources.transform(ifSaEntries)

  def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaEmployments] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaEmployments.transform(ifSaEntries)

  def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                   (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSelfEmployments] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSelfEmployments.transform(ifSaEntries)

  def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                  (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFurtherDetails] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFurtherDetails.transform(ifSaEntries)

}