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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework

import play.api.libs.json.{Format, JsPath}

case class IfPaye(paye: Seq[IfPayeEntry])

object IfPaye {

  val minValue = -999999999.99
  val maxValue = 999999999.99
  val minValueTaxDeductedOrRefunded = -9999999999.99
  val maxValueTaxDeductedOrRefunded = 9999999999.99
  val minPositiveValue = 0
  val payeWholeUnitsPaymentTypeMinValue = -99999
  val payeWholeUnitsPaymentTypeMaxValue = 99999
  val payeWholeUnitsPositivePaymentTypeMaxValue = 99999

  def isMultipleOfPointZeroOne(value: Double): Boolean = (BigDecimal(value) * 100.0) % 1 == 0

  def isInRange(value: Double): Boolean = value >= minValue && value <= maxValue

  def isInRangeTaxDeductedOrRefunded(value: Double): Boolean = value >= minValueTaxDeductedOrRefunded && value <= maxValueTaxDeductedOrRefunded

  def isInPositiveRange(value: Double): Boolean = value >= minPositiveValue && value <= maxValue

  def isInRangeWholeUnits(value: Double): Boolean =
    value >= payeWholeUnitsPaymentTypeMinValue && value <= payeWholeUnitsPaymentTypeMaxValue

  def isInRangePositiveWholeUnits(value: Double): Boolean =
    value >= minPositiveValue && value <= payeWholeUnitsPositivePaymentTypeMaxValue

  def paymentAmountValidator(value: Double): Boolean =
    isInRange(value) && isMultipleOfPointZeroOne(value)

  def paymentAmountValidatorTaxDeductedOrRefunded(value: Double): Boolean =
    isInRangeTaxDeductedOrRefunded(value) && isMultipleOfPointZeroOne(value)

  def positivePaymentAmountValidator(value: Double): Boolean =
    isInPositiveRange(value) && isMultipleOfPointZeroOne(value)

  def payeWholeUnitsPaymentTypeValidator(value: Int): Boolean = isInRangeWholeUnits(value)
  def payeWholeUnitsPositivePaymentTypeValidator(value: Int): Boolean = isInRangePositiveWholeUnits(value)

  implicit val incomePayeFormat: Format[IfPaye] = Format(
    (JsPath \ "paye").read[Seq[IfPayeEntry]].map(value => IfPaye(value)),
    (JsPath \ "paye").write[Seq[IfPayeEntry]].contramap(value => value.paye)
  )
}