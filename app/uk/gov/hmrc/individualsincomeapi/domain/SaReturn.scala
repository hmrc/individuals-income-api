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

case class SaAnnualReturns(taxYear: TaxYear, annualReturns: Seq[SaReturn])
case class SaReturn(receivedDate: LocalDate)

object SaAnnualReturns {
  def apply(desSaIncome: DesSAIncome): SaAnnualReturns = {
    SaAnnualReturns(TaxYear.fromEndYear(desSaIncome.taxYear.toInt), desSaIncome.returnList.map(_.receivedDate) map SaReturn)
  }
}

case class SaAnnualEmployments(taxYear: TaxYear, employments: Seq[SaEmploymentsIncome])
case class SaEmploymentsIncome(employmentIncome: Double)

object SaAnnualEmployments {
  def apply(desSaIncome: DesSAIncome): SaAnnualEmployments = {
    SaAnnualEmployments(TaxYear.fromEndYear(desSaIncome.taxYear.toInt), desSaIncome.returnList.map(_.incomeFromAllEmployments.getOrElse(0.0)) map SaEmploymentsIncome)
  }
}

case class SaAnnualSelfEmployments(taxYear: TaxYear, selfEmployments: Seq[SaSelfEmploymentsIncome])
case class SaSelfEmploymentsIncome(selfEmploymentStartDate: Option[LocalDate], selfEmploymentIncome: Double)

object SaAnnualSelfEmployments {
  def apply(desSAIncome: DesSAIncome): SaAnnualSelfEmployments = {
    SaAnnualSelfEmployments(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(x => SaSelfEmploymentsIncome(x.selfEmploymentStartDate, x.selfEmploymentIncome.getOrElse(0.0))))
  }
}