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

package uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox

import uk.gov.hmrc.individualsincomeapi.domain.integrationframework._

case class SandboxIncomePaye() {
  def createValidPayeEntry(date: String) =
    IfPayeEntry(
      taxCode = Some("K971"),
      paidHoursWorked = Some("36"),
      payePositivePaymentType = Some(19157.5),
      totalTaxToDate = Some(3095.89),
      taxDeductedOrRefunded = Some(159228.49),
      grossEarningsForNics = Some(createValodIFGrossEarningsForNics),
      employerPayeRef = Some("345/34678"),
      paymentDate = Some(date),
      taxablePay = None,
      taxYear = Some("18-19"),
      monthlyPeriodNumber = None,
      weeklyPeriodNumber = None,
      payFrequency = Some("W4"),
      dednsFromNetPay = Some(198035.8),
      totalEmployerNics = Some(createValidTotalEmployerNics()),
      employeeNics = Some(createValidEmployeeNics()),
      employeePensionContribs = Some(createValidEmployeePensionContribs()),
      benefits = None,
      Some(createValidStatutoryPayYTD()),
      studentLoan = None,
      postGradLoan = None,
      Some(createValidAdditionalFields())
    )

  private def createValidAdditionalFields() =
    IfAdditionalFields(Some(false), Some("yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"))

  private def createValidStatutoryPayYTD() =
    IfStatutoryPayYTD(Some(15797.45), Some(13170.69), Some(16193.76), Some(30846.56))

  private def createValidEmployeeNics() =
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

  private def createValidTotalEmployerNics() =
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

  private def createValidEmployeePensionContribs() =
    IfEmployeePensionContribs(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))

  private def createValodIFGrossEarningsForNics() =
    IfGrossEarningsForNics(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))
}
