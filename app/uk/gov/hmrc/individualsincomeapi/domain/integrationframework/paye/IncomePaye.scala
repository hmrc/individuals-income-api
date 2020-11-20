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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.Reads.{maxLength, minLength, pattern, verifying}
import play.api.libs.json.{Format, JsPath, Json, Reads}

case class IncomePaye(paye: Seq[PayeEntry])

object IncomePaye {

  val minValue = -9999999999.99
  val maxValue = 9999999999.99

  val studentLoanPlanTypePattern = "^(01|02)$".r
  val taxCodePattern = "^([1-9][0-9]{0,5}[LMNPTY])|(BR)|(0T)|(NT)|(D[0-8])|([K][1-9][0-9]{0,5})$".r
  val paidHoursWorkPattern = "^[^ ].{0,34}$".r
  val employerPayeRefPattern = "^[^ ].{1,9}$".r
  val paymentDatePattern = ("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)" +
    "[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}" +
    "[-]02[-](0[1-9]|1[0-9]|2[0-8])))$").r

  val payeTaxYearPattern = "^[0-9]{2}\\-[0-9]{2}$".r
  val monthlyPeriodNumberPattern = "^([1-9]|1[0-2])$".r
  val weeklyPeriodNumberPattern = "^([1-9]|[1-4][0-9]|5[0-46])$".r

  val payFrequencyValues = Seq("W1", "W2", "W4", "M1", "M3", "M6", "MA", "IO", "IR")

  def isInPayFrequency(implicit rds: Reads[String]) : Reads[String] = {
    verifying(value => payFrequencyValues.contains(value))
  }

  def isMultipleOfPointZeroOne(value: Double): Boolean = (BigDecimal(value) * 100.0) % 1 == 0

  def isInRange(value: Double): Boolean = value > minValue && value < maxValue

  def paymentAmountValidator(implicit rds: Reads[Double]): Reads[Double] =
    verifying[Double](value => isInRange(value) && isMultipleOfPointZeroOne(value))

  implicit val postGradLoanFormat: Format[PostGradLoan] = Format(
    (
      (JsPath \ "repaymentsInPayPeriod").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "repaymentsYTD").readNullable[Double](paymentAmountValidator)
      ) (PostGradLoan.apply _),
    (
      (JsPath \ "repaymentsInPayPeriod").writeNullable[Double] and
        (JsPath \ "repaymentsYTD").writeNullable[Double]
      ) (unlift(PostGradLoan.unapply))
  )

  implicit val studentLoanFormat: Format[StudentLoan] = Format(
    (
      (JsPath \ "planType").readNullable[String]
        (pattern(studentLoanPlanTypePattern, "Invalid student loan plan type")) and
        (JsPath \ "repaymentsInPayPeriod").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "repaymentsYTD").readNullable[Double](paymentAmountValidator)
      ) (StudentLoan.apply _),
    (
      (JsPath \ "planType").writeNullable[String] and
        (JsPath \ "repaymentsInPayPeriod").writeNullable[Double] and
        (JsPath \ "repaymentsYTD").writeNullable[Double]
      ) (unlift(StudentLoan.unapply))
  )

  implicit val benefitsFormat: Format[Benefits] = Format(
    (
      (JsPath \ "taxedViaPayroll").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "taxedViaPayrollYTD").readNullable[Double](paymentAmountValidator)
      ) (Benefits.apply _),
    (
      (JsPath \ "taxedViaPayroll").writeNullable[Double] and
        (JsPath \ "taxedViaPayrollYTD").writeNullable[Double]
      ) (unlift(Benefits.unapply))
  )

  implicit val employeePensionContribsFormat: Format[EmployeePensionContribs] = Format(
    (
      (JsPath \ "paidYTD").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "notPaidYTD").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "paid").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "notPaid").readNullable[Double](paymentAmountValidator)
      ) (EmployeePensionContribs.apply _),
    (
      (JsPath \ "paidYTD").writeNullable[Double] and
        (JsPath \ "notPaidYTD").writeNullable[Double] and
        (JsPath \ "paid").writeNullable[Double] and
        (JsPath \ "notPaid").writeNullable[Double]
      ) (unlift(EmployeePensionContribs.unapply))
  )

  implicit val grossEarningsForNicsFormat: Format[GrossEarningsForNics] = Format(
    (
      (JsPath \ "inPayPeriod1").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "inPayPeriod2").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "inPayPeriod3").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "inPayPeriod4").readNullable[Double](paymentAmountValidator)
      ) (GrossEarningsForNics.apply _),
    (
      (JsPath \ "inPayPeriod1").writeNullable[Double] and
        (JsPath \ "inPayPeriod2").writeNullable[Double] and
        (JsPath \ "inPayPeriod3").writeNullable[Double] and
        (JsPath \ "inPayPeriod4").writeNullable[Double]
      ) (unlift(GrossEarningsForNics.unapply))
  )

  implicit val employeeNicsFormat: Format[EmployeeNics] = Format(
    (
      (JsPath \ "inPayPeriod1").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "inPayPeriod2").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "inPayPeriod3").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "inPayPeriod4").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ytd1").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ytd2").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ytd3").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ytd4").readNullable[Double](paymentAmountValidator)
      ) (EmployeeNics.apply _),
    (
      (JsPath \ "inPayPeriod1").writeNullable[Double] and
        (JsPath \ "inPayPeriod2").writeNullable[Double] and
        (JsPath \ "inPayPeriod3").writeNullable[Double] and
        (JsPath \ "inPayPeriod4").writeNullable[Double] and
        (JsPath \ "ytd1").writeNullable[Double] and
        (JsPath \ "ytd2").writeNullable[Double] and
        (JsPath \ "ytd3").writeNullable[Double] and
        (JsPath \ "ytd4").writeNullable[Double]
      ) (unlift(EmployeeNics.unapply))
  )

  implicit val payeEntryFormat: Format[PayeEntry] = Format(
    (
      (JsPath \ "taxCode").readNullable[String]
        (minLength[String](2)
          .keepAnd(maxLength[String](7)
            .keepAnd(pattern(taxCodePattern, "Invalid Tax Code")))) and
        (JsPath \ "paidHoursWorked").readNullable[String]
          (maxLength[String](35)
            .keepAnd(pattern(paidHoursWorkPattern, "Invalid Paid Hours Work"))) and
        (JsPath \ "taxablePayToDate").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "totalTaxToDate").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "taxDeductedOrRefunded").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "employerPayeRef").readNullable[String]
          (maxLength[String](10)
            .keepAnd(pattern(employerPayeRefPattern, "Invalid employer PAYE reference"))) and
        (JsPath \ "paymentDate").readNullable[String](pattern(paymentDatePattern, "Invalid Payment Date")) and
        (JsPath \ "taxablePay").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "taxYear").readNullable[String](pattern(payeTaxYearPattern, "Invalid Tax Year")) and
        (JsPath \ "monthlyPeriodNumber").readNullable[String]
          (pattern(monthlyPeriodNumberPattern, "Invalid Monthly Period Number")
            .keepAnd(minLength[String](1)).keepAnd(maxLength[String](2))) and
        (JsPath \ "weeklyPeriodNumber").readNullable[String]
          (pattern(weeklyPeriodNumberPattern, "Invalid Weekly Period Number")
            .keepAnd(minLength[String](1)).keepAnd(maxLength[String](2))) and
        (JsPath \ "payFrequency").readNullable[String](isInPayFrequency) and
        (JsPath \ "dednsFromNetPay").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "employeeNICs").readNullable[EmployeeNics] and
        (JsPath \ "employeePensionContribs").readNullable[EmployeePensionContribs] and
        (JsPath \ "benefits").readNullable[Benefits] and
        (JsPath \ "statutoryPayYTD" \ "parentalBereavement").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "studentLoan").readNullable[StudentLoan] and
        (JsPath \ "postGradLoan").readNullable[PostGradLoan]
      ) (PayeEntry.apply _),
    (
      (JsPath \ "taxCode").writeNullable[String] and
        (JsPath \ "paidHoursWorked").writeNullable[String] and
        (JsPath \ "taxablePayToDate").writeNullable[Double] and
        (JsPath \ "totalTaxToDate").writeNullable[Double] and
        (JsPath \ "taxDeductedOrRefunded").writeNullable[Double] and
        (JsPath \ "employerPayeRef").writeNullable[String] and
        (JsPath \ "paymentDate").writeNullable[String] and
        (JsPath \ "taxablePay").writeNullable[Double] and
        (JsPath \ "taxYear").writeNullable[String] and
        (JsPath \ "monthlyPeriodNumber").writeNullable[String] and
        (JsPath \ "weeklyPeriodNumber").writeNullable[String] and
        (JsPath \ "payFrequency").writeNullable[String] and
        (JsPath \ "dednsFromNetPay").writeNullable[Double] and
        (JsPath \ "employeeNICs").writeNullable[EmployeeNics] and
        (JsPath \ "employeePensionContribs").writeNullable[EmployeePensionContribs] and
        (JsPath \ "benefits").writeNullable[Benefits] and
        (JsPath \ "statutoryPayYTD" \ "parentalBereavement").writeNullable[Double] and
        (JsPath \ "studentLoan").writeNullable[StudentLoan] and
        (JsPath \ "postGradLoan").writeNullable[PostGradLoan]
      ) (unlift(PayeEntry.unapply))
  )

  implicit val incomePayeFormat: Format[IncomePaye] = Format(
    (JsPath \ "paye").read[Seq[PayeEntry]].map(value => IncomePaye(value)),
    (JsPath \ "paye").write[Seq[PayeEntry]].contramap(value => value.paye)
  )

  val incomePayeEntryFormat = Json.format[IncomePayeEntry]
}