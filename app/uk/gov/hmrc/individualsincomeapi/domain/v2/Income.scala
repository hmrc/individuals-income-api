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

package uk.gov.hmrc.individualsincomeapi.domain.v2

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework._

case class Income(
  employerPayeReference: Option[String],
  taxYear: Option[String],
  employee: Option[Employee],
  payroll: Option[Payroll],
  payFrequency: Option[String],
  monthPayNumber: Option[Int],
  weekPayNumber: Option[Int],
  paymentDate: Option[String],
  paidHoursWorked: Option[String],
  taxCode: Option[String],
  taxablePayToDate: Option[Double],
  taxablePay: Option[Double],
  totalTaxToDate: Option[Double],
  taxDeductedOrRefunded: Option[Double],
  dednsFromNetPay: Option[Double],
  employeePensionContribs: Option[IfEmployeePensionContribs],
  statutoryPayYTD: Option[IfStatutoryPayYTD],
  grossEarningsForNics: Option[IfGrossEarningsForNics],
  totalEmployerNics: Option[IfTotalEmployerNics],
  employeeNics: Option[IfEmployeeNics])

object Income {
  implicit val incomeJsonFormat: Format[Income] = Json.format[Income]
}
