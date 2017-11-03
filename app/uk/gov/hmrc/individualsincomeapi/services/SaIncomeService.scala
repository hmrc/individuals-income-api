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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.findByMatchId
import uk.gov.hmrc.individualsincomeapi.domain._

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.ExecutionContext.Implicits.global

trait SaIncomeService {
  def fetchSaReturnsByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturn]]

  def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]]

  def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualSelfEmployments]]
}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

  override def fetchSaReturnsByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturn]] = {
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) =>
        val selectedSaReturns = saIncomes.filter(s => s.isIn(taxYearInterval)).sortBy(_.taxYear.toInt).reverse
        successful(selectedSaReturns map (r => SaTaxReturn(r)))
      case None => failed(new MatchNotFoundException)
    }
  }

  override def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]] = {
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) =>
        val selectedSaReturns = saIncomes.filter(s => s.isIn(taxYearInterval)).sortBy(_.taxYear.toInt).reverse
        successful(selectedSaReturns map (r => SaAnnualEmployments(r)))
      case None => failed(new MatchNotFoundException)
    }
  }

  override def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualSelfEmployments]] = {
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncomes) =>
        val selectedSaReturns = saIncomes.filter(s => s.isIn(taxYearInterval)).sortBy(_.taxYear.toInt).reverse
        successful(selectedSaReturns map (r => SaAnnualSelfEmployments(r)))
      case None => failed(new MatchNotFoundException)
    }
  }
}

@Singleton
class LiveSaIncomeService @Inject()(matchingConnector: IndividualsMatchingApiConnector, desConnector: DesConnector) extends SaIncomeService {

  override def fetchSaReturnsByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaTaxReturn]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- desConnector.fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaTaxReturn(r))
  }

  override def fetchEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier): Future[Seq[SaAnnualEmployments]] = {
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desSaIncomes <- desConnector.fetchSelfAssessmentIncome(ninoMatch.nino, taxYearInterval)
    } yield desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaAnnualEmployments(r))
  }

  override def fetchSelfEmploymentsIncomeByMatchId(matchId: UUID, taxYearInterval: TaxYearInterval)(implicit hc: HeaderCarrier) = ???
}
