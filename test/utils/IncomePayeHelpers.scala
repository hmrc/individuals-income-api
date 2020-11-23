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

package utils

import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.{IFBenefits, IFEmployeeNics, IFEmployeePensionContribs, IFGrossEarningsForNics, IFPayeEntry, IFPostGradLoan, IFStudentLoan, IFTotalEmployerNics}

trait IncomePayeHelpers {
  def createValidPayeEntry() =
    IFPayeEntry(
      Some("K971"),
      Some("36"),
      Some(19157.5),
      Some(3095.89),
      Some(159228.49),
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
      Some(39708.7),
      Some(createValidStudentLoan()),
      Some(createValidPostGradLoan())
    )

  private def createValidEmployeeNics() =
    IFEmployeeNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  private def createValidTotalEmployerNics() =
    IFTotalEmployerNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  private def createValidEmployeePensionContribs() =
    IFEmployeePensionContribs(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))

  private def createValidBenefits() = IFBenefits(Some(506328.1), Some(246594.83))

  private def createValidStudentLoan() = IFStudentLoan(Some("02"), Some(88478.16), Some(545.52))

  private def createValidPostGradLoan() = IFPostGradLoan(Some(15636.22), Some(46849.26))

  private def createValodIFGrossEarningsForNics() =
    IFGrossEarningsForNics(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))
}
