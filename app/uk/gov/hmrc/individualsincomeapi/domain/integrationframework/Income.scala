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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IncomeSa.{isInRange, isMultipleOfPointZeroOne}

case class Address(
                    line1: Option[String],
                    line2: Option[String],
                    line3: Option[String],
                    line4: Option[String],
                    line5: Option[String] = None,
                    postcode: Option[String]
                  )

case class PostGradLoan(repaymentsInPayPeriod: Option[Double], repaymentsYtd: Option[Double])

case class StudentLoan(
                        planType: Option[String],
                        repaymentsInPayPeriod: Option[Double],
                        repaymentsYTD: Option[Double]
                      )

case class Benefits(taxedViaPayroll: Option[Double], taxedViaPayrollYtd: Option[Double])

case class EmployeePensionContribs(
                                    paidYtd: Option[Double],
                                    notPaidYtd: Option[Double],
                                    paid: Option[Double],
                                    notPaid: Option[Double]
                                  )

case class GrossEarningsForNics(
                                 inPayPeriod1: Option[Double],
                                 inPayPeriod2: Option[Double],
                                 inPayPeriod3: Option[Double],
                                 inPayPeriod4: Option[Double]
                               )

case class EmployeeNics(
                         inPayPeriod1: Option[Double],
                         inPayPeriod2: Option[Double],
                         inPayPeriod3: Option[Double],
                         inPayPeriod4: Option[Double],
                         ytd1: Option[Double],
                         ytd2: Option[Double],
                         ytd3: Option[Double],
                         ytd4: Option[Double]
                       )

case class PayeEntry(
                      taxCode: Option[String],
                      paidHoursWorked: Option[String],
                      taxablePayToDate: Option[Double],
                      totalTaxToDate: Option[Double],
                      taxDeductedOrRefunded: Option[Double],
                      employerPayeRef: Option[String],
                      paymentDate: Option[String],
                      taxablePay: Option[Double],
                      taxYear: Option[String],
                      monthlyPeriodNumber: Option[String],
                      weeklyPeriodNumber: Option[String],
                      payFrequency: Option[String],
                      dednsFromNetPay: Option[Double],
                      employeeNics: Option[EmployeeNics],
                      employeePensionContribs: Option[EmployeePensionContribs],
                      benefits: Option[Benefits],
                      parentalBereavement: Option[Double],
                      studentLoan: Option[StudentLoan],
                      postGradLoan: Option[PostGradLoan]
                    )

case class SaIncome(
                     selfAssessment: Option[Double],
                     allEmployments: Option[Double],
                     ukInterest: Option[Double],
                     foreignDivs: Option[Double],
                     ukDivsAndInterest: Option[Double],
                     partnerships: Option[Double],
                     pensions: Option[Double],
                     selfEmployment: Option[Double],
                     trusts: Option[Double],
                     ukProperty: Option[Double],
                     foreign: Option[Double],
                     lifePolicies: Option[Double],
                     shares: Option[Double],
                     other: Option[Double]
                   )

case class SaReturnType(
                         utr: Option[String],
                         caseStartDate: Option[String],
                         receivedDate: Option[String],
                         businessDescription: Option[String],
                         telephoneNumber: Option[String],
                         busStartDate: Option[String],
                         busEndDate: Option[String],
                         totalTaxPaid: Option[Double],
                         totalNIC: Option[Double],
                         turnover: Option[Double],
                         otherBusinessIncome: Option[Double],
                         tradingIncomeAllowance: Option[Double],
                         address: Option[Address],
                         income: Option[SaIncome]
                       )

case class SaTaxYearEntry(taxYear: Option[String], income: Option[Double], returnList: Option[Seq[SaReturnType]])

case class IncomeSaEntry(incomeSa: IncomeSa)

case class IncomePayeEntry(incomePaye: IncomePaye)

case class IncomeSa(sa: Seq[SaTaxYearEntry])

case class IncomePaye(paye: Seq[PayeEntry])

object IncomeSa {

  val minValue = -9999999999.99
  val maxValue = 9999999999.99

  val utrPattern = "^[0-9]{10}$".r
  val dateStringPattern = ("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)" +
    "[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-]" +
    "(0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-]" +
    "(0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-]" +
    "(0[1-9]|1[0-9]|2[0-8])))$").r

  val taxYearPattern = "^20[0-9]{2}$".r

  def isMultipleOfPointZeroOne(value: Double): Boolean = (BigDecimal(value) * 100.0) % 1 == 0

  def isInRange(value: Double): Boolean = value > minValue && value < maxValue

  def paymentAmountValidator(implicit rds: Reads[Double]): Reads[Double] =
    verifying[Double](value => isInRange(value) && isMultipleOfPointZeroOne(value))

  implicit val addressFormat: Format[Address] = Format(
    (
      (JsPath \ "line1").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line2").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line3").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line4").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line5").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "postcode").readNullable[String](minLength[String](1).andKeep(maxLength[String](10)))
      ) (Address.apply _),
    (
      (JsPath \ "line1").writeNullable[String] and
        (JsPath \ "line2").writeNullable[String] and
        (JsPath \ "line3").writeNullable[String] and
        (JsPath \ "line4").writeNullable[String] and
        (JsPath \ "line5").writeNullable[String] and
        (JsPath \ "postcode").writeNullable[String]
      ) (unlift(Address.unapply))
  )

  implicit val saIncomeFormat: Format[SaIncome] = Format(
    (
      (JsPath \ "selfAssessment").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "allEmployments").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ukInterest").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "foreignDivs").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ukDivsAndInterest").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "partnerships").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "pensions").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "selfEmployment").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "trusts").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "ukProperty").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "foreign").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "lifePolicies").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "shares").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "other").readNullable[Double](paymentAmountValidator)
      ) (SaIncome.apply _),
    (
      (JsPath \ "selfAssessment").writeNullable[Double] and
        (JsPath \ "allEmployments").writeNullable[Double] and
        (JsPath \ "ukInterest").writeNullable[Double] and
        (JsPath \ "foreignDivs").writeNullable[Double] and
        (JsPath \ "ukDivsAndInterest").writeNullable[Double] and
        (JsPath \ "partnerships").writeNullable[Double] and
        (JsPath \ "pensions").writeNullable[Double] and
        (JsPath \ "selfEmployment").writeNullable[Double] and
        (JsPath \ "trusts").writeNullable[Double] and
        (JsPath \ "ukProperty").writeNullable[Double] and
        (JsPath \ "foreign").writeNullable[Double] and
        (JsPath \ "lifePolicies").writeNullable[Double] and
        (JsPath \ "shares").writeNullable[Double] and
        (JsPath \ "other").writeNullable[Double]
      ) (unlift(SaIncome.unapply))
  )

  implicit val saReturnTypeFormat: Format[SaReturnType] = Format(
    (
      (JsPath \ "utr").readNullable[String](pattern(utrPattern, "Invalid UTR")) and
        (JsPath \ "caseStartDate").readNullable[String]
          (pattern(dateStringPattern, "Invalid Case Start Date")) and
        (JsPath \ "receivedDate").readNullable[String]
          (pattern(dateStringPattern, "Invalid Received Date")) and
        (JsPath \ "businessDescription").readNullable[String]
          (minLength[String](0).keepAnd(maxLength[String](100))) and
        (JsPath \ "telephoneNumber").readNullable[String]
          (minLength[String](0).keepAnd(maxLength[String](100))) and
        (JsPath \ "busStartDate").readNullable[String]
          (pattern(dateStringPattern, "Invalid Business Start Date")) and
        (JsPath \ "busEndDate").readNullable[String]
          (pattern(dateStringPattern, "Invalid Business End Date")) and
        (JsPath \ "totalTaxPaid").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "totalNIC").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "turnover").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "otherBusIncome").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "tradingIncomeAllowance").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "address").readNullable[Address] and
        (JsPath \ "income").readNullable[SaIncome]
      ) (SaReturnType.apply _),
    (
      (JsPath \ "utr").writeNullable[String] and
        (JsPath \ "caseStartDate").writeNullable[String] and
        (JsPath \ "receivedDate").writeNullable[String] and
        (JsPath \ "businessDescription").writeNullable[String] and
        (JsPath \ "telephoneNumber").writeNullable[String] and
        (JsPath \ "busStartDate").writeNullable[String] and
        (JsPath \ "busEndDate").writeNullable[String] and
        (JsPath \ "totalTaxPaid").writeNullable[Double] and
        (JsPath \ "totalNIC").writeNullable[Double] and
        (JsPath \ "turnover").writeNullable[Double] and
        (JsPath \ "otherBusIncome").writeNullable[Double] and
        (JsPath \ "tradingIncomeAllowance").writeNullable[Double] and
        (JsPath \ "address").writeNullable[Address] and
        (JsPath \ "income").writeNullable[SaIncome]
      ) (unlift(SaReturnType.unapply))
  )

  implicit val saTaxYearEntryFormat: Format[SaTaxYearEntry] = Format(
    (
      (JsPath \ "taxYear").readNullable[String](pattern(taxYearPattern, "Invalid Tax Year")) and
        (JsPath \ "income").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "returnList").readNullable[Seq[SaReturnType]]
      ) (SaTaxYearEntry.apply _),
    (
      (JsPath \ "taxYear").writeNullable[String] and
        (JsPath \ "income").writeNullable[Double] and
        (JsPath \ "returnList").writeNullable[Seq[SaReturnType]]
      ) (unlift(SaTaxYearEntry.unapply))
  )

  implicit val incomeSaFormat: Format[IncomeSa] = Format(
    (JsPath \ "sa").read[Seq[SaTaxYearEntry]].map(value => IncomeSa(value)),
    (JsPath \ "sa").write[Seq[SaTaxYearEntry]].contramap(value => value.sa)
  )

  //  val incomeSaEntryFormat: Format[IncomeSaEntry] = Format(
  //    (
  //      (JsPath \ "id").read[Id] and
  //        (JsPath \ "incomeSaResponse").read[IncomeSa]
  //      )(IncomeSaEntry.apply _),
  //    (
  //      (JsPath \ "id").write[Id] and
  //        (JsPath \ "incomeSaResponse").write[IncomeSa]
  //      )(unlift(IncomeSaEntry.unapply))
  //  )

  val incomeSaEntryFormat = Json.format[IncomeSaEntry]
}

object IncomePaye {
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

  //  val incomePayeEntryFormat: Format[IncomePayeEntry] = Format(
  //    (
  //      (JsPath \ "id").read[Id] and
  //        (JsPath \ "incomePaye").read[IncomePaye]
  //      )(IncomePayeEntry.apply _),
  //    (
  //      (JsPath \ "id").write[Id] and
  //        (JsPath \ "incomePaye").write[IncomePaye]
  //      )(unlift(IncomePayeEntry.unapply))
  //  )

  val incomePayeEntryFormat = Json.format[IncomePayeEntry]
}
