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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{Format, JsPath}
import play.api.libs.json.Reads.{maxLength, minLength, pattern}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa.IFSa.paymentAmountValidator

case class IFAddress(
  line1: Option[String],
  line2: Option[String],
  line3: Option[String],
  line4: Option[String],
  line5: Option[String] = None,
  postcode: Option[String]
)

case class IFSaIncome(
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

case class IFSaReturn(
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
  address: Option[IFAddress],
  income: Option[IFSaIncome]
)

case class IFSaEntry(
  taxYear: Option[String],
  income: Option[Double],
  returnList: Option[Seq[IFSaReturn]]
)

object IFSaEntry {

  val taxYearPattern = "^20[0-9]{2}$".r

  val dateStringPattern = ("^(((19|20)([2468][048]|[13579][26]|0[48])|2000)" +
    "[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-]" +
    "(0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-]" +
    "(0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-]" +
    "(0[1-9]|1[0-9]|2[0-8])))$").r

  val utrPattern = "^[0-9]{10}$".r

  implicit val addressFormat: Format[IFAddress] = Format(
    (
      (JsPath \ "line1").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line2").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line3").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line4").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "line5").readNullable[String](minLength[String](0).andKeep(maxLength[String](100))) and
        (JsPath \ "postcode").readNullable[String](minLength[String](1).andKeep(maxLength[String](10)))
    )(IFAddress.apply _),
    (
      (JsPath \ "line1").writeNullable[String] and
        (JsPath \ "line2").writeNullable[String] and
        (JsPath \ "line3").writeNullable[String] and
        (JsPath \ "line4").writeNullable[String] and
        (JsPath \ "line5").writeNullable[String] and
        (JsPath \ "postcode").writeNullable[String]
    )(unlift(IFAddress.unapply))
  )

  implicit val saIncomeFormat: Format[IFSaIncome] = Format(
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
    )(IFSaIncome.apply _),
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
    )(unlift(IFSaIncome.unapply))
  )

  implicit val saReturnTypeFormat: Format[IFSaReturn] = Format(
    (
      (JsPath \ "utr").readNullable[String](pattern(utrPattern, "Invalid UTR")) and
        (JsPath \ "caseStartDate").readNullable[String](pattern(dateStringPattern, "Invalid Case Start Date")) and
        (JsPath \ "receivedDate").readNullable[String](pattern(dateStringPattern, "Invalid Received Date")) and
        (JsPath \ "businessDescription").readNullable[String](minLength[String](0).keepAnd(maxLength[String](100))) and
        (JsPath \ "telephoneNumber").readNullable[String](minLength[String](0).keepAnd(maxLength[String](100))) and
        (JsPath \ "busStartDate").readNullable[String](pattern(dateStringPattern, "Invalid Business Start Date")) and
        (JsPath \ "busEndDate").readNullable[String](pattern(dateStringPattern, "Invalid Business End Date")) and
        (JsPath \ "totalTaxPaid").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "totalNIC").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "turnover").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "otherBusIncome").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "tradingIncomeAllowance").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "address").readNullable[IFAddress] and
        (JsPath \ "income").readNullable[IFSaIncome]
    )(IFSaReturn.apply _),
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
        (JsPath \ "address").writeNullable[IFAddress] and
        (JsPath \ "income").writeNullable[IFSaIncome]
    )(unlift(IFSaReturn.unapply))
  )

  implicit val saTaxYearEntryFormat: Format[IFSaEntry] = Format(
    (
      (JsPath \ "taxYear").readNullable[String](pattern(taxYearPattern, "Invalid Tax Year")) and
        (JsPath \ "income").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "returnList").readNullable[Seq[IFSaReturn]]
    )(IFSaEntry.apply _),
    (
      (JsPath \ "taxYear").writeNullable[String] and
        (JsPath \ "income").writeNullable[Double] and
        (JsPath \ "returnList").writeNullable[Seq[IFSaReturn]]
    )(unlift(IFSaEntry.unapply))
  )

}
