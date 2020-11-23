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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa.IFIncomeSa.paymentAmountValidator

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

object IFSaIncome {

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

}
