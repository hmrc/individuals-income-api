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

package uk.gov.hmrc.individualsincomeapi.services.v1

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.des.DesSAIncome
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.findByMatchId
import uk.gov.hmrc.individualsincomeapi.domain.v1._

import java.util.UUID
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

trait SaIncomeService {
  def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[SaFootprint]

  def fetchReturnsSummary(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaTaxReturnSummaries]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaTaxReturnSummaries(desSAIncome))

  def fetchTrustsIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualTrustIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualTrustIncomes(desSAIncome))

  def fetchForeignIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualForeignIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualForeignIncomes(desSAIncome))

  def fetchPartnershipsIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualPartnershipIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualPartnershipIncomes(desSAIncome))

  def fetchInterestsAndDividendsIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualInterestAndDividendIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualInterestAndDividendIncomes(desSAIncome))

  def fetchUkPropertiesIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualUkPropertiesIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualUkPropertiesIncomes(desSAIncome))

  def fetchPensionsAndStateBenefitsIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualPensionAndStateBenefitIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualPensionAndStateBenefitIncomes(desSAIncome))

  def fetchEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualEmployments]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualEmployments(desSAIncome))

  def fetchSelfEmploymentsIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualSelfEmployments]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualSelfEmployments(desSAIncome))

  def fetchAdditionalInformation(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualAdditionalInformations]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualAdditionalInformations(desSAIncome))

  def fetchOtherIncome(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaAnnualOtherIncomes]] =
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualOtherIncomes(desSAIncome))

  def fetchSaIncomeSources(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[SaIncomeSources]] =
    fetchSaIncomes(matchId, taxYearInterval)(SaIncomeSources.apply)

  protected def fetchSaIncomes[T](matchId: UUID, taxYearInterval: TaxYearInterval)(transform: DesSAIncome => T)(implicit
    hc: HeaderCarrier
  ): Future[Seq[T]]
}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

  protected def fetchSaIncomes[T](matchId: UUID, taxYearInterval: TaxYearInterval)(
    transform: DesSAIncome => T
  )(implicit hc: HeaderCarrier): Future[Seq[T]] =
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) =>
        val selectedSaReturns = saIncomes.filter(s => s.isIn(taxYearInterval)).sortBy(_.taxYear.toInt).reverse
        successful(selectedSaReturns map (r => transform(r)))
      case None => failed(new MatchNotFoundException)
    }

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[SaFootprint] =
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) => successful(SaFootprint(saIncomes.filter(s => s.isIn(taxYearInterval))))
      case None            => failed(new MatchNotFoundException)
    }
}

@Singleton
class LiveSaIncomeService @Inject() (
  matchingConnector: IndividualsMatchingApiConnector,
  desConnector: DesConnector,
  cacheService: CacheService,
  @Named("retryDelay") retryDelay: Int
)(implicit ec: ExecutionContext)
    extends SaIncomeService {

  private def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[Seq[DesSAIncome]] = {
    val cacheId = SaCacheId(nino, taxYearInterval)
    cacheService
      .get[Seq[DesSAIncome]](cacheId, withRetry(desConnector.fetchSelfAssessmentIncome(nino, taxYearInterval)))
  }

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case UpstreamErrorResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }

  protected def fetchSaIncomes[T](matchId: UUID, taxYearInterval: TaxYearInterval)(
    transform: DesSAIncome => T
  )(implicit hc: HeaderCarrier): Future[Seq[T]] =
    for {
      ninoMatch    <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => transform(r))

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit
    hc: HeaderCarrier
  ): Future[SaFootprint] =
    for {
      ninoMatch    <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield SaFootprint(desSaIncomes)
}
