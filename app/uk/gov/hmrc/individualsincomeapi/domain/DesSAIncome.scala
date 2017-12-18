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

case class DesSAIncome(taxYear: String,
                       returnList: Seq[DesSAReturn]) {

  def isIn(taxYearInterval: TaxYearInterval) = taxYear.toInt >= taxYearInterval.fromTaxYear.endYr && taxYear.toInt <= taxYearInterval.toTaxYear.endYr
}

case class DesSAReturn(caseStartDate: LocalDate,
                       receivedDate: LocalDate,
                       utr: SaUtr,
                       incomeFromAllEmployments: Option[Double] = None,
                       profitFromSelfEmployment: Option[Double] = None,
                       incomeFromSelfAssessment: Option[Double] = None,
                       incomeFromTrust: Option[Double] = None,
                       incomeFromForeign4Sources: Option[Double] = None,
                       profitFromPartnerships: Option[Double] = None,
                       incomeFromUkInterest: Option[Double] = None,
                       incomeFromForeignDividends: Option[Double] = None,
                       incomeFromInterestNDividendsFromUKCompaniesNTrusts: Option[Double] = None,
                       incomeFromProperty: Option[Double] = None
                      )
