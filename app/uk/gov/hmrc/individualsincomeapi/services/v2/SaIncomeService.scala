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

import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, TaxYearInterval}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2.{SaAdditionalInformationRecords, SaEmployments, SaFootprint, SaForeignIncomes, SaFurtherDetails, SaInterestAndDividends, SaOtherIncomeRecords, SaPartnerships, SaPensionAndStateBenefits, SaSelfEmployments, SaSources, SaSummaries, SaTrusts, SaUkProperties}
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomeData.findByMatchId

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.{failed, successful}

trait SaIncomeService {

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

  def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                      (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFootprint]

  def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                  (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSummaries]

  def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                 (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaTrusts]

  def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                  (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaForeignIncomes]

  def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                       (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaPartnerships]

  def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaInterestAndDividends]

  def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                  (implicit hc: HeaderCarrier,
                                   request: RequestHeader): Future[SaPensionAndStateBenefits]

  def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                       (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaUkProperties]

  def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier,
                                 request: RequestHeader): Future[SaAdditionalInformationRecords]

  def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                      (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaOtherIncomeRecords]

  def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                  (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSources]

  def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                      (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaEmployments]

  def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                          (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSelfEmployments]

  def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                         (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFurtherDetails]
}

@Singleton
class LiveSaIncomeService @Inject()(matchingConnector: IndividualsMatchingApiConnector,
                                    ifConnector: IfConnector,
                                    cache: SaIncomeCacheService,
                                    scopeService: ScopesService,
                                    scopesHelper: ScopesHelper,
                                    @Named("retryDelay") retryDelay: Int)
                                   (implicit ec: ExecutionContext) extends SaIncomeService {

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

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFootprint] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFootprint.transform(ifSaEntries)

  override def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSummaries] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSummaries.transform(ifSaEntries)

  override def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                          (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaTrusts] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaTrusts.transform(ifSaEntries)

  override def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaForeignIncomes] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaForeignIncomes.transform(ifSaEntries)

  override def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaPartnerships] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaPartnerships.transform(ifSaEntries)

  override def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                        (implicit hc: HeaderCarrier,
                                         request: RequestHeader): Future[SaInterestAndDividends] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaInterestAndDividends.transform(ifSaEntries)

  override def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                           (implicit hc: HeaderCarrier,
                                            request: RequestHeader): Future[SaPensionAndStateBenefits] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaPensionAndStateBenefits.transform(ifSaEntries)

  override def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaUkProperties] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaUkProperties.transform(ifSaEntries)

  override def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                         (implicit hc: HeaderCarrier,
                                          request: RequestHeader): Future[SaAdditionalInformationRecords] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaAdditionalInformationRecords.transform(ifSaEntries)

  override def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaOtherIncomeRecords] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaOtherIncomeRecords.transform(ifSaEntries)

  override def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSources] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSources.transform(ifSaEntries)

  override def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaEmployments] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaEmployments.transform(ifSaEntries)

  override def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                   (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSelfEmployments] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSelfEmployments.transform(ifSaEntries)

  override def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                  (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFurtherDetails] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFurtherDetails.transform(ifSaEntries)

}

@Singleton
class SandboxSaIncomeService @Inject()(implicit ec: ExecutionContext) extends SaIncomeService {

  private def fetchSaIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[Seq[IfSaEntry]] =
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncome) => successful(saIncome)
      case None => failed(new MatchNotFoundException)
    }

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFootprint] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(ifEntry => SaFootprint.transform(ifEntry))

  override def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSummaries] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaSummaries.transform(ifEntry)
    )

  override def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                          (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaTrusts] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaTrusts.transform(ifEntry)
    )

  override def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaForeignIncomes] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaForeignIncomes.transform(ifEntry)
    )

  override def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaPartnerships] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaPartnerships.transform(ifEntry)
    )

  override def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                        (implicit hc: HeaderCarrier,
                                         request: RequestHeader): Future[SaInterestAndDividends] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaInterestAndDividends.transform(ifEntry)
    )

  override def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                           (implicit hc: HeaderCarrier,
                                            request: RequestHeader): Future[SaPensionAndStateBenefits] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaPensionAndStateBenefits.transform(ifEntry)
    )

  override def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaUkProperties] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaUkProperties.transform(ifEntry)
    )

  override def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                         (implicit hc: HeaderCarrier,
                                          request: RequestHeader): Future[SaAdditionalInformationRecords] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaAdditionalInformationRecords.transform(ifEntry)
    )

  override def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaOtherIncomeRecords] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaOtherIncomeRecords.transform(ifEntry)
    )

  override def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                           (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSources] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaSources.transform(ifEntry)
    )

  override def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                               (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaEmployments] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaEmployments.transform(ifEntry)
    )

  override def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                   (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaSelfEmployments] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaSelfEmployments.transform(ifEntry)
    )

  override def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])
                                  (implicit hc: HeaderCarrier, request: RequestHeader): Future[SaFurtherDetails] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaFurtherDetails.transform(ifEntry)
    )

}
