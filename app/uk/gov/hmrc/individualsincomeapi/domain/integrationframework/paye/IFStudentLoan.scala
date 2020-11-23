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

import play.api.libs.json.{Format, JsPath}
import play.api.libs.json.Reads.pattern
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IFIncomePaye.{paymentAmountValidator}
import play.api.libs.functional.syntax.{unlift, _}

case class IFStudentLoan(
  planType: Option[String],
  repaymentsInPayPeriod: Option[Double],
  repaymentsYTD: Option[Double]
)

object IFStudentLoan {

  val studentLoanPlanTypePattern = "^(01|02)$".r

  implicit val studentLoanFormat: Format[IFStudentLoan] = Format(
    (
      (JsPath \ "planType")
        .readNullable[String](pattern(studentLoanPlanTypePattern, "Invalid student loan plan type")) and
        (JsPath \ "repaymentsInPayPeriod").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "repaymentsYTD").readNullable[Double](paymentAmountValidator)
    )(IFStudentLoan.apply _),
    (
      (JsPath \ "planType").writeNullable[String] and
        (JsPath \ "repaymentsInPayPeriod").writeNullable[Double] and
        (JsPath \ "repaymentsYTD").writeNullable[Double]
    )(unlift(IFStudentLoan.unapply))
  )

}
