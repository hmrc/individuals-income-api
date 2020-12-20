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
import play.api.libs.json.{Format, JsPath, Json, Reads}
import play.api.libs.json.Reads.{maxLength, minLength, pattern, verifying}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IfPaye._
import uk.gov.hmrc.individualsincomeapi.domain.v2.{Employee, Income, Payroll}

case class IfGrossEarningsForNics(
  inPayPeriod1: Option[Double],
  inPayPeriod2: Option[Double],
  inPayPeriod3: Option[Double],
  inPayPeriod4: Option[Double]
)

case class IfTotalEmployerNics(
  inPayPeriod1: Option[Double],
  inPayPeriod2: Option[Double],
  inPayPeriod3: Option[Double],
  inPayPeriod4: Option[Double],
  ytd1: Option[Double],
  ytd2: Option[Double],
  ytd3: Option[Double],
  ytd4: Option[Double]
)

case class IfEmployeeNics(
  inPayPeriod1: Option[Double],
  inPayPeriod2: Option[Double],
  inPayPeriod3: Option[Double],
  inPayPeriod4: Option[Double],
  ytd1: Option[Double],
  ytd2: Option[Double],
  ytd3: Option[Double],
  ytd4: Option[Double]
)

case class IfEmployeePensionContribs(
  paidYtd: Option[Double],
  notPaidYtd: Option[Double],
  paid: Option[Double],
  notPaid: Option[Double]
)

case class IfBenefits(
  taxedViaPayroll: Option[Double],
  taxedViaPayrollYtd: Option[Double]
)

case class IfStatutoryPayYTD(
  maternity: Option[Double],
  paternity: Option[Double],
  adoption: Option[Double],
  parentalBereavement: Option[Double]
)

case class IfStudentLoan(
  planType: Option[String],
  repaymentsInPayPeriod: Option[Int],
  repaymentsYTD: Option[Int]
)

case class IfPostGradLoan(
  repaymentsInPayPeriod: Option[Int],
  repaymentsYtd: Option[Int]
)

case class IFAdditionalFields(
                             employeeHasPartner: Option[Boolean],
                             payrollId: Option[String]
                             )

case class IfPayeEntry(
  taxCode: Option[String],
  paidHoursWorked: Option[String],
  taxablePayToDate: Option[Double],
  totalTaxToDate: Option[Double],
  taxDeductedOrRefunded: Option[Double],
  grossEarningsForNics: Option[IfGrossEarningsForNics],
  employerPayeRef: Option[String],
  paymentDate: Option[String],
  taxablePay: Option[Double],
  taxYear: Option[String],
  monthlyPeriodNumber: Option[String],
  weeklyPeriodNumber: Option[String],
  payFrequency: Option[String],
  dednsFromNetPay: Option[Double],
  totalEmployerNics: Option[IfTotalEmployerNics],
  employeeNics: Option[IfEmployeeNics],
  employeePensionContribs: Option[IfEmployeePensionContribs],
  benefits: Option[IfBenefits],
  statutoryPayYTD: Option[IfStatutoryPayYTD],
  studentLoan: Option[IfStudentLoan],
  postGradLoan: Option[IfPostGradLoan],
  additionalFields: Option[IFAdditionalFields]
)

object IfPayeEntry {

  val taxCodePattern = "^([1-9][0-9]{0,5}[LMNPTY])|(BR)|(0T)|(NT)|(D[0-8])|([K][1-9][0-9]{0,5})$".r
  val paidHoursWorkPattern = "^[^ ].{0,34}$".r
  val employerPayeRefPattern = "^[^ ].{1,9}$".r
  val paymentDatePattern = ("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)" +
    "[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}" +
    "[-]02[-](0[1-9]|1[0-9]|2[0-8])))$").r

  val payeTaxYearPattern = "^[0-9]{2}\\-[0-9]{2}$".r
  val monthlyPeriodNumberPattern = "^([1-9]|1[0-2])$".r
  val weeklyPeriodNumberPattern = "^([1-9]|[1-4][0-9]|5[0-46])$".r

  val studentLoanPlanTypePattern = "^(01|02)$".r

  val payFrequencyValues = Seq("W1", "W2", "W4", "M1", "M3", "M6", "MA", "IO", "IR")

  def isInPayFrequency(implicit rds: Reads[String]): Reads[String] =
    verifying(value => payFrequencyValues.contains(value))

  implicit val grossEarningsForNicsFormat: Format[IfGrossEarningsForNics] = Format(
    (
      (JsPath \ "inPayPeriod1").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod2").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod3").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod4").readNullable[Double](verifying(paymentAmountValidator))
    )(IfGrossEarningsForNics.apply _),
    (
      (JsPath \ "inPayPeriod1").writeNullable[Double] and
        (JsPath \ "inPayPeriod2").writeNullable[Double] and
        (JsPath \ "inPayPeriod3").writeNullable[Double] and
        (JsPath \ "inPayPeriod4").writeNullable[Double]
    )(unlift(IfGrossEarningsForNics.unapply))
  )

  implicit val totalEmployerNicsFormat: Format[IfTotalEmployerNics] = Format(
    (
      (JsPath \ "inPayPeriod1").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod2").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod3").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod4").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd1").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd2").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd3").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd4").readNullable[Double](verifying(paymentAmountValidator))
    )(IfTotalEmployerNics.apply _),
    (
      (JsPath \ "inPayPeriod1").writeNullable[Double] and
        (JsPath \ "inPayPeriod2").writeNullable[Double] and
        (JsPath \ "inPayPeriod3").writeNullable[Double] and
        (JsPath \ "inPayPeriod4").writeNullable[Double] and
        (JsPath \ "ytd1").writeNullable[Double] and
        (JsPath \ "ytd2").writeNullable[Double] and
        (JsPath \ "ytd3").writeNullable[Double] and
        (JsPath \ "ytd4").writeNullable[Double]
    )(unlift(IfTotalEmployerNics.unapply))
  )

  implicit val employeeNicsFormat: Format[IfEmployeeNics] = Format(
    (
      (JsPath \ "inPayPeriod1").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod2").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod3").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "inPayPeriod4").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd1").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd2").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd3").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "ytd4").readNullable[Double](verifying(paymentAmountValidator))
    )(IfEmployeeNics.apply _),
    (
      (JsPath \ "inPayPeriod1").writeNullable[Double] and
        (JsPath \ "inPayPeriod2").writeNullable[Double] and
        (JsPath \ "inPayPeriod3").writeNullable[Double] and
        (JsPath \ "inPayPeriod4").writeNullable[Double] and
        (JsPath \ "ytd1").writeNullable[Double] and
        (JsPath \ "ytd2").writeNullable[Double] and
        (JsPath \ "ytd3").writeNullable[Double] and
        (JsPath \ "ytd4").writeNullable[Double]
    )(unlift(IfEmployeeNics.unapply))
  )

  implicit val employeePensionContribsFormat: Format[IfEmployeePensionContribs] = Format(
    (
      (JsPath \ "paidYTD").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "notPaidYTD").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "paid").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "notPaid").readNullable[Double](verifying(paymentAmountValidator))
    )(IfEmployeePensionContribs.apply _),
    (
      (JsPath \ "paidYTD").writeNullable[Double] and
        (JsPath \ "notPaidYTD").writeNullable[Double] and
        (JsPath \ "paid").writeNullable[Double] and
        (JsPath \ "notPaid").writeNullable[Double]
    )(unlift(IfEmployeePensionContribs.unapply))
  )

  implicit val benefitsFormat: Format[IfBenefits] = Format(
    (
      (JsPath \ "taxedViaPayroll").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "taxedViaPayrollYTD").readNullable[Double](verifying(paymentAmountValidator))
    )(IfBenefits.apply _),
    (
      (JsPath \ "taxedViaPayroll").writeNullable[Double] and
        (JsPath \ "taxedViaPayrollYTD").writeNullable[Double]
    )(unlift(IfBenefits.unapply))
  )

  implicit val statutoryPayYTDFormat: Format[IfStatutoryPayYTD] = Format(
    (
      (JsPath \ "maternity").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "paternity").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "adoption").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "parentalBereavement").readNullable[Double](verifying(paymentAmountValidator))
    )(IfStatutoryPayYTD.apply _),
    (
      (JsPath \ "maternity").writeNullable[Double] and
        (JsPath \ "paternity").writeNullable[Double] and
        (JsPath \ "adoption").writeNullable[Double] and
        (JsPath \ "parentalBereavement").writeNullable[Double]
    )(unlift(IfStatutoryPayYTD.unapply))
  )

  implicit val studentLoanFormat: Format[IfStudentLoan] = Format(
    (
      (JsPath \ "planType")
        .readNullable[String](pattern(studentLoanPlanTypePattern, "Invalid student loan plan type")) and
        (JsPath \ "repaymentsInPayPeriod").readNullable[Int](verifying(payeWholeUnitsPaymentTypeValidator)) and
        (JsPath \ "repaymentsYTD").readNullable[Int](verifying(payeWholeUnitsPositivePaymentTypeValidator))
    )(IfStudentLoan.apply _),
    (
      (JsPath \ "planType").writeNullable[String] and
        (JsPath \ "repaymentsInPayPeriod").writeNullable[Int] and
        (JsPath \ "repaymentsYTD").writeNullable[Int]
    )(unlift(IfStudentLoan.unapply))
  )

  implicit val postGradLoanFormat: Format[IfPostGradLoan] = Format(
    (
      (JsPath \ "repaymentsInPayPeriod").readNullable[Int](verifying(payeWholeUnitsPaymentTypeValidator)) and
        (JsPath \ "repaymentsYTD").readNullable[Int](verifying(payeWholeUnitsPositivePaymentTypeValidator))
    )(IfPostGradLoan.apply _),
    (
      (JsPath \ "repaymentsInPayPeriod").writeNullable[Int] and
        (JsPath \ "repaymentsYTD").writeNullable[Int]
    )(unlift(IfPostGradLoan.unapply))
  )

  implicit val additionalFieldsFormat: Format[IFAdditionalFields] = Format(
    (
      (JsPath \ "employee" \ "hasPartner").readNullable[Boolean] and
        (JsPath \ "payroll" \ "id").readNullable[String]
      )(IFAdditionalFields.apply _),
    (
      (JsPath \ "employee" \ "hasPartner").writeNullable[Boolean] and
        (JsPath \ "payroll" \ "id").writeNullable[String]
      )(unlift(IFAdditionalFields.unapply))
  )

  implicit val payeEntryFormat: Format[IfPayeEntry] = Format(
    (
      (JsPath \ "taxCode").readNullable[String](minLength[String](2)
        .keepAnd(maxLength[String](7)
          .keepAnd(pattern(taxCodePattern, "Invalid Tax Code")))) and
        (JsPath \ "paidHoursWorked").readNullable[String](maxLength[String](35)
          .keepAnd(pattern(paidHoursWorkPattern, "Invalid Paid Hours Work"))) and
        (JsPath \ "taxablePayToDate").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "totalTaxToDate").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "taxDeductedOrRefunded").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "grossEarningsForNICs").readNullable[IfGrossEarningsForNics] and
        (JsPath \ "employerPayeRef").readNullable[String](maxLength[String](10)
          .keepAnd(pattern(employerPayeRefPattern, "Invalid employer PAYE reference"))) and
        (JsPath \ "paymentDate").readNullable[String](pattern(paymentDatePattern, "Invalid Payment Date")) and
        (JsPath \ "taxablePay").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "taxYear").readNullable[String](pattern(payeTaxYearPattern, "Invalid Tax Year")) and
        (JsPath \ "monthlyPeriodNumber").readNullable[String]
          (pattern(monthlyPeriodNumberPattern, "Invalid Monthly Period Number")
            .keepAnd(minLength[String](1)).keepAnd(maxLength[String](2))) and
        (JsPath \ "weeklyPeriodNumber").readNullable[String]
          (pattern(weeklyPeriodNumberPattern, "Invalid Weekly Period Number")
            .keepAnd(minLength[String](1)).keepAnd(maxLength[String](2))) and
        (JsPath \ "payFrequency").readNullable[String](isInPayFrequency) and
        (JsPath \ "dednsFromNetPay").readNullable[Double](verifying(paymentAmountValidator)) and
        (JsPath \ "totalEmployerNICs").readNullable[IfTotalEmployerNics] and
        (JsPath \ "employeeNICs").readNullable[IfEmployeeNics] and
        (JsPath \ "employeePensionContribs").readNullable[IfEmployeePensionContribs] and
        (JsPath \ "benefits").readNullable[IfBenefits] and
        (JsPath \ "statutoryPayYTD").readNullable[IfStatutoryPayYTD] and
        (JsPath \ "studentLoan").readNullable[IfStudentLoan] and
        (JsPath \ "postGradLoan").readNullable[IfPostGradLoan] and
        JsPath.readNullable[IFAdditionalFields]
    )(IfPayeEntry.apply _),
    (
      (JsPath \ "taxCode").writeNullable[String] and
        (JsPath \ "paidHoursWorked").writeNullable[String] and
        (JsPath \ "taxablePayToDate").writeNullable[Double] and
        (JsPath \ "totalTaxToDate").writeNullable[Double] and
        (JsPath \ "taxDeductedOrRefunded").writeNullable[Double] and
        (JsPath \ "grossEarningsForNICs").writeNullable[IfGrossEarningsForNics] and
        (JsPath \ "employerPayeRef").writeNullable[String] and
        (JsPath \ "paymentDate").writeNullable[String] and
        (JsPath \ "taxablePay").writeNullable[Double] and
        (JsPath \ "taxYear").writeNullable[String] and
        (JsPath \ "monthlyPeriodNumber").writeNullable[String] and
        (JsPath \ "weeklyPeriodNumber").writeNullable[String] and
        (JsPath \ "payFrequency").writeNullable[String] and
        (JsPath \ "dednsFromNetPay").writeNullable[Double] and
        (JsPath \ "totalEmployerNICs").writeNullable[IfTotalEmployerNics] and
        (JsPath \ "employeeNICs").writeNullable[IfEmployeeNics] and
        (JsPath \ "employeePensionContribs").writeNullable[IfEmployeePensionContribs] and
        (JsPath \ "benefits").writeNullable[IfBenefits] and
        (JsPath \ "statutoryPayYTD").writeNullable[IfStatutoryPayYTD] and
        (JsPath \ "studentLoan").writeNullable[IfStudentLoan] and
        (JsPath \ "postGradLoan").writeNullable[IfPostGradLoan] and
        JsPath.writeNullable[IFAdditionalFields]
    )(unlift(IfPayeEntry.unapply))
  )

  private def toEmployee(additionalFields: Option[IFAdditionalFields]): Option[Employee] = {

    additionalFields match {
      case Some(fields) => {
        fields.employeeHasPartner match {
          case Some(hasPartner) => Some(Employee(Some(hasPartner)))
          case None => None
        }
      }
      case None => None
    }

  }

  private def toPayroll(additionalFields: Option[IFAdditionalFields]): Option[Payroll] = {

    additionalFields match {
      case Some(fields) => {
        fields.payrollId match {
          case Some(payrollId) => Some(Payroll(Some(payrollId)))
          case None => None
        }
      }
      case None => None
    }

  }

  def toIncome(paye: IfPayeEntry): Income =

      Income(
        paye.employerPayeRef,
        paye.taxYear,
        toEmployee(paye.additionalFields),
        toPayroll(paye.additionalFields),
        paye.payFrequency,
        paye.monthlyPeriodNumber,
        paye.weeklyPeriodNumber,
        paye.paymentDate,
        paye.paidHoursWorked,
        paye.taxCode,
        paye.taxablePayToDate,
        paye.totalTaxToDate,
        paye.taxDeductedOrRefunded,
        paye.dednsFromNetPay,
        paye.employeePensionContribs,
        paye.statutoryPayYTD,
        paye.grossEarningsForNics,
        paye.totalEmployerNics,
        paye.employeeNics
      )

}
