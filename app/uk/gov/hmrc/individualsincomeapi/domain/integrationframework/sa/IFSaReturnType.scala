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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa.IFIncomeSa.{dateStringPattern, paymentAmountValidator, utrPattern}

case class IFSaReturnType(
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

object IFSaReturnType {

  implicit val saReturnTypeFormat: Format[IFSaReturnType] = Format(
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
    )(IFSaReturnType.apply _),
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
    )(unlift(IFSaReturnType.unapply))
  )

}
