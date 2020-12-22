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

package uk.gov.hmrc.individualsincomeapi.services.v2

import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, TaxYearInterval}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2.{SaAdditionalInformationRecords, SaEmployments, SaFootprint, SaForeignIncomes, SaFurtherDetails, SaInterestAndDividends, SaOtherIncomeRecords, SaPartnerships, SaPensionAndStateBenefits, SaSelfEmployments, SaSources, SaSummaries, SaTrusts, SaUkProperties}
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomeData.findByMatchId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait SaIncomeService {

  def endpoints =
    List(
      "incomeSa",
      "incomeSaSummary",
      "incomeSaTrusts",
      "incomeSaForeign",
      "incomeSaPartnerships",
      "incomeSaInterestsAndDividends",
      "incomeSaPensionsAndStateBenefits",
      "incomeSaUkProperties",
      "incomeSaAdditionalInformation",
      "incomeSaOther",
      "incomeSaSource",
      "incomeSaEmployments",
      "incomeSaSelfEmployments",
      "incomeSaFurtherDetails"
    )

  def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFootprint]

  def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSummaries]

  def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaTrusts]

  def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaForeignIncomes]

  def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaPartnerships]

  def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaInterestAndDividends]

  def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaPensionAndStateBenefits]

  def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaUkProperties]

  def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaAdditionalInformationRecords]

  def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaOtherIncomeRecords]

  def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSources]

  def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaEmployments]

  def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSelfEmployments]

  def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFurtherDetails]
}

@Singleton
class LiveSaIncomeService @Inject()(
  matchingConnector: IndividualsMatchingApiConnector,
  ifConnector: IfConnector,
  cache: SaIncomeCacheService,
  scopeService: ScopesService,
  scopesHelper: ScopesHelper,
  @Named("retryDelay") retryDelay: Int)
    extends SaIncomeService {

  private def fetchSaIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[IfSaEntry]] =
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
                       Option(scopesHelper.getQueryStringFor(scopes.toList, endpoints)).filter(_.nonEmpty)
                     )
                   )
                 )
    } yield saIncome

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFootprint] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFootprint.transform(ifSaEntries)

  override def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSummaries] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSummaries.transform(ifSaEntries)

  override def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaTrusts] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaTrusts.transform(ifSaEntries)

  override def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaForeignIncomes] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaForeignIncomes.transform(ifSaEntries)

  override def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaPartnerships] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaPartnerships.transform(ifSaEntries)

  override def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaInterestAndDividends] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaInterestAndDividends.transform(ifSaEntries)

  override def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaPensionAndStateBenefits] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaPensionAndStateBenefits.transform(ifSaEntries)

  override def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaUkProperties] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaUkProperties.transform(ifSaEntries)

  override def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaAdditionalInformationRecords] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaAdditionalInformationRecords.transform(ifSaEntries)

  override def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaOtherIncomeRecords] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaOtherIncomeRecords.transform(ifSaEntries)

  override def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSources] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSources.transform(ifSaEntries)

  override def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaEmployments] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaEmployments.transform(ifSaEntries)

  override def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSelfEmployments] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaSelfEmployments.transform(ifSaEntries)

  override def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFurtherDetails] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFurtherDetails.transform(ifSaEntries)

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case Upstream5xxResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }

}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

  private def fetchSaIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[IfSaEntry]] =
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncome) => successful(saIncome)
      case None           => failed(new MatchNotFoundException)
    }

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFootprint] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(ifEntry => SaFootprint.transform(ifEntry))

  override def fetchSummary(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSummaries] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaSummaries.transform(ifEntry)
    )

  override def fetchTrusts(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaTrusts] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaTrusts.transform(ifEntry)
    )

  override def fetchForeign(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaForeignIncomes] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaForeignIncomes.transform(ifEntry)
    )

  override def fetchPartnerships(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaPartnerships] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaPartnerships.transform(ifEntry)
    )

  override def fetchInterestAndDividends(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaInterestAndDividends] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaInterestAndDividends.transform(ifEntry)
    )

  override def fetchPensionAndStateBenefits(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaPensionAndStateBenefits] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaPensionAndStateBenefits.transform(ifEntry)
    )

  override def fetchUkProperties(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaUkProperties] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaUkProperties.transform(ifEntry)
    )

  override def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaAdditionalInformationRecords] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaAdditionalInformationRecords.transform(ifEntry)
    )

  override def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaOtherIncomeRecords] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaOtherIncomeRecords.transform(ifEntry)
    )

  override def fetchSources(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSources] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaSources.transform(ifEntry)
    )

  override def fetchEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaEmployments] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaEmployments.transform(ifEntry)
    )

  override def fetchSelfEmployments(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaSelfEmployments] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaSelfEmployments.transform(ifEntry)
    )

  override def fetchFurtherDetails(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFurtherDetails] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(
      ifEntry => SaFurtherDetails.transform(ifEntry)
    )

}
