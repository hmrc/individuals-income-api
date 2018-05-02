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

package uk.gov.hmrc.individualsincomeapi.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.cache.{SaCacheId, SaIncomeCacheService}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.findByMatchId
import uk.gov.hmrc.individualsincomeapi.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait SaIncomeService {
  def fetchSaFootprintByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[SaFootprint]

  def fetchSaReturnsSummaryByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturnSummaries]]

  def fetchSaTrustsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualTrustIncomes]]

  def fetchSaForeignIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualForeignIncomes]]

  def fetchSaPartnershipsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPartnershipIncomes]]

  def fetchSaInterestsAndDividendsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualInterestAndDividendIncomes]]

  def fetchSaUkPropertiesIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualUkPropertiesIncomes]]

  def fetchSaPensionsAndStateBenefitsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPensionAndStateBenefitIncomes]]

  def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]]

  def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualSelfEmployments]]

  def fetchSaAdditionalInformationByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualAdditionalInformations]]

  def fetchSaOtherIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualOtherIncomes]]
}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

  private def fetchSaIncomes[T](matchId: UUID, taxYearInterval: TaxYearInterval)(transform: DesSAIncome => T): Future[Seq[T]] = {
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) =>
        val selectedSaReturns = saIncomes.filter(s => s.isIn(taxYearInterval)).sortBy(_.taxYear.toInt).reverse
        successful(selectedSaReturns map (r => transform(r)))
      case None => failed(new MatchNotFoundException)
    }
  }

  override def fetchSaFootprintByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[SaFootprint] = {
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) => successful(SaFootprint(saIncomes.filter(s => s.isIn(taxYearInterval))))
      case None => failed(new MatchNotFoundException)
    }
  }

  override def fetchSaReturnsSummaryByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturnSummaries]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaTaxReturnSummaries(desSAIncome))
  }

  override def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualEmployments(desSAIncome))
  }

  override def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualSelfEmployments]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualSelfEmployments(desSAIncome))
  }

  override def fetchSaTrustsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualTrustIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualTrustIncomes(desSAIncome))
  }

  override def fetchSaForeignIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualForeignIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualForeignIncomes(desSAIncome))
  }

  override def fetchSaPartnershipsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPartnershipIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualPartnershipIncomes(desSAIncome))
  }

  override def fetchSaInterestsAndDividendsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualInterestAndDividendIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualInterestAndDividendIncomes(desSAIncome))
  }

  override def fetchSaPensionsAndStateBenefitsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPensionAndStateBenefitIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualPensionAndStateBenefitIncomes(desSAIncome))
  }

  override def fetchSaUkPropertiesIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualUkPropertiesIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualUkPropertiesIncomes(desSAIncome))
  }

  override def fetchSaAdditionalInformationByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier) = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualAdditionalInformations(desSAIncome))
  }

  override def fetchSaOtherIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualOtherIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualOtherIncomes(desSAIncome))
  }
}

@Singleton
class LiveSaIncomeService @Inject()(matchingConnector: IndividualsMatchingApiConnector, desConnector: DesConnector, saIncomeCacheService: SaIncomeCacheService) extends SaIncomeService {

  private def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[DesSAIncome]] = {
    val cacheId = SaCacheId(nino, taxYearInterval)
    saIncomeCacheService.get[Seq[DesSAIncome]](cacheId, desConnector.fetchSelfAssessmentIncome(nino, taxYearInterval))
  }

  private def fetchSaIncomes[T](matchId: UUID, taxYearInterval: TaxYearInterval)(transform: DesSAIncome => T)(implicit hc: HeaderCarrier): Future[Seq[T]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => transform(r))
  }

  override def fetchSaFootprintByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[SaFootprint] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield SaFootprint(desSaIncomes)
  }

  override def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualEmployments(desSAIncome))
  }

  override def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier) = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualSelfEmployments(desSAIncome))
  }

  override def fetchSaReturnsSummaryByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturnSummaries]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaTaxReturnSummaries(desSAIncome))
  }

  override def fetchSaTrustsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualTrustIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualTrustIncomes(desSAIncome))
  }

  override def fetchSaForeignIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualForeignIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualForeignIncomes(desSAIncome))
  }

  override def fetchSaPartnershipsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPartnershipIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualPartnershipIncomes(desSAIncome))
  }

  override def fetchSaInterestsAndDividendsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualInterestAndDividendIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualInterestAndDividendIncomes(desSAIncome))
  }

  override def fetchSaPensionsAndStateBenefitsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPensionAndStateBenefitIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualPensionAndStateBenefitIncomes(desSAIncome))
  }

  override def fetchSaUkPropertiesIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualUkPropertiesIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualUkPropertiesIncomes(desSAIncome))
  }

  override def fetchSaAdditionalInformationByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier) = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualAdditionalInformations(desSAIncome))
  }

  override def fetchSaOtherIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualOtherIncomes]] = {
    fetchSaIncomes(matchId, taxYearInterval)(desSAIncome => SaAnnualOtherIncomes(desSAIncome))
  }
}
