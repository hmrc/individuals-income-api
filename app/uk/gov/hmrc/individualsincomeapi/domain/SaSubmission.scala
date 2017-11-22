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

package uk.gov.hmrc.individualsincomeapi.domain

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.SaUtr

case class SaFootprint(registrations: Seq[SaRegistration], taxReturns: Seq[SaTaxReturn])

case class SaTaxReturn(taxYear: TaxYear, submissions: Seq[SaSubmission])
case class SaSubmission(utr: SaUtr, receivedDate: LocalDate)
case class SaRegistration(utr: SaUtr, registrationDate: LocalDate)

object SaTaxReturn {
  def apply(desSaIncome: DesSAIncome): SaTaxReturn = {
    SaTaxReturn(TaxYear.fromEndYear(desSaIncome.taxYear.toInt), desSaIncome.returnList.map(r => SaSubmission(r.utr, r.receivedDate)))
  }
}

object SaFootprint {
  implicit def dateTimeOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def apply(desSaIncomes: Seq[DesSAIncome]): SaFootprint = {
    val saRegistrations = desSaIncomes.flatMap(_.returnList map (saReturn => SaRegistration(saReturn.utr, saReturn.caseStartDate))).toSet
    val saReturns = desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaTaxReturn(r))

    SaFootprint(saRegistrations.toSeq.sortBy(_.registrationDate).reverse, saReturns)
  }
}

case class SaAnnualEmployments(taxYear: TaxYear, employments: Seq[SaEmploymentsIncome])
case class SaEmploymentsIncome(utr: SaUtr, employmentIncome: Double)

object SaAnnualEmployments {
  def apply(desSaIncome: DesSAIncome): SaAnnualEmployments = {
    SaAnnualEmployments(TaxYear.fromEndYear(desSaIncome.taxYear.toInt), desSaIncome.returnList.map(sa => SaEmploymentsIncome(sa.utr, sa.incomeFromAllEmployments.getOrElse(0.0))))
  }
}

case class SaAnnualSelfEmployments(taxYear: TaxYear, selfEmployments: Seq[SaSelfEmploymentsIncome])
case class SaSelfEmploymentsIncome(utr: SaUtr, selfEmploymentProfit: Double)

object SaAnnualSelfEmployments {
  def apply(desSAIncome: DesSAIncome): SaAnnualSelfEmployments = {
    SaAnnualSelfEmployments(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa => SaSelfEmploymentsIncome(sa.utr, sa.profitFromSelfEmployment.getOrElse(0.0))))
  }
}

case class SaTaxReturnSummaries(taxYear: TaxYear, summary: Seq[SaTaxReturnSummary])
case class SaTaxReturnSummary(utr: SaUtr, totalIncome: Double)

object SaTaxReturnSummaries {
  def apply(desSAIncome: DesSAIncome): SaTaxReturnSummaries = {
    SaTaxReturnSummaries(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa => SaTaxReturnSummary(sa.utr, sa.incomeFromSelfAssessment.getOrElse(0.0))))
  }
}