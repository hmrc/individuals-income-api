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

package utils

import uk.gov.hmrc.individualsincomeapi.domain.integrationframework._

trait IncomePayeHelpers {

  def createInvalidPayeEntry() =
    createValidPayeEntry().copy(payFrequency = Some("MADEUPVAL"))

  def createInvalidTaxPayableToDate() =
    createValidPayeEntry().copy(taxablePayToDate = Some(-99999))

  def createValidPayeEntry(): IfPayeEntry =
    IfPayeEntry(
      Some("K971"),
      Some("36"),
      Some(19157.5),
      Some(3095.89),
      Some(9999999999.99),
      Some(createValodIFGrossEarningsForNics),
      Some("345/34678"),
      Some("2006-02-27"),
      Some(16533.95),
      Some("18-19"),
      Some("3"),
      Some("2"),
      Some("W4"),
      Some(198035.8),
      Some(createValidTotalEmployerNics()),
      Some(createValidEmployeeNics()),
      Some(createValidEmployeePensionContribs()),
      Some(createValidBenefits()),
      Some(createValidStatutoryPayToDate()),
      Some(createValidStudentLoan()),
      Some(createValidPostGradLoan()),
      Some(createValidAdditionalFields())
    )

  def createValidPayeEntryNegative(): IfPayeEntry =
    IfPayeEntry(
      Some("K971"),
      Some("36"),
      Some(19157.5),
      Some(3095.89),
      Some(-9999999999.99),
      Some(createValodIFGrossEarningsForNics),
      Some("345/34678"),
      Some("2006-02-27"),
      Some(16533.95),
      Some("18-19"),
      Some("3"),
      Some("2"),
      Some("W4"),
      Some(198035.8),
      Some(createValidTotalEmployerNics()),
      Some(createValidEmployeeNics()),
      Some(createValidEmployeePensionContribs()),
      Some(createValidBenefits()),
      Some(createValidStatutoryPayToDate()),
      Some(createValidStudentLoan()),
      Some(createValidPostGradLoan()),
      Some(createValidAdditionalFields())
    )

  private def createValidAdditionalFields() =
    IfAdditionalFields(Some(false), Some("yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"))

  def createValidStatutoryPayToDate() =
    IfStatutoryPayYTD(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56)
    )

  def createValidEmployeeNics() =
    IfEmployeeNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  def createValidTotalEmployerNics() =
    IfTotalEmployerNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  def createValidEmployeePensionContribs() =
    IfEmployeePensionContribs(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))

  def createValidBenefits() = IfBenefits(Some(506328.1), Some(246594.83))

  def createValidStudentLoan() = IfStudentLoan(Some("02"), Some(88478), Some(545))

  def createValidPostGradLoan() = IfPostGradLoan(Some(15636), Some(46849))

  def createValodIFGrossEarningsForNics() =
    IfGrossEarningsForNics(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))
}
