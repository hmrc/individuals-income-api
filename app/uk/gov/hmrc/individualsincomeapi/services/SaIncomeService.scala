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

  def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]]

  def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualSelfEmployments]]
}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

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

  private def fetchSaIncomes[T](matchId: UUID, taxYearInterval: TaxYearInterval)(transform: DesSAIncome => T): Future[Seq[T]] = {
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) =>
        val selectedSaReturns = saIncomes.filter(s => s.isIn(taxYearInterval)).sortBy(_.taxYear.toInt).reverse
        successful(selectedSaReturns map (r => transform(r)))
      case None => failed(new MatchNotFoundException)
    }
  }
}

@Singleton
class LiveSaIncomeService @Inject()(matchingConnector: IndividualsMatchingApiConnector, desConnector: DesConnector, saIncomeCacheService: SaIncomeCacheService) extends SaIncomeService {

  private def fetchSelfAssessmentIncome(nino: Nino, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[DesSAIncome]] = {
    val cacheId = SaCacheId(nino, taxYearInterval)
    saIncomeCacheService.get[Seq[DesSAIncome]](cacheId, desConnector.fetchSelfAssessmentIncome(nino, taxYearInterval))
  }

  override def fetchSaFootprintByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[SaFootprint] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield SaFootprint(desSaIncomes)
  }

  override def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaAnnualEmployments(r))
  }

  override def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier) = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaAnnualSelfEmployments(r))
  }

  override def fetchSaReturnsSummaryByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturnSummaries]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaTaxReturnSummaries(r))
  }

  override def fetchSaTrustsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualTrustIncomes]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaAnnualTrustIncomes(r))
  }

  override def fetchSaForeignIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualForeignIncomes]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaAnnualForeignIncomes(r))
  }

  override def fetchSaPartnershipsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualPartnershipIncomes]] = ???
}
