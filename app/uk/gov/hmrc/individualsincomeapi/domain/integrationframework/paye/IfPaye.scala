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
import play.api.libs.json.Reads.verifying
import play.api.libs.json.{Format, JsPath, Reads}
import uk.gov.hmrc.individualsincomeapi.domain.v2.Income

case class IfPaye(paye: Seq[IfPayeEntry])

object IfPaye {

  val minValue = -9999999999.99
  val maxValue = 9999999999.99
  val payeWholeUnitsPaymentTypeMinValue = -99999
  val payeWholeUnitsPaymentTypeMaxValue = 99999
  val payeWholeUnitsPositivePaymentTypeMinValue = 0
  val payeWholeUnitsPositivePaymentTypeMaxValue = 99999

  def isMultipleOfPointZeroOne(value: Double): Boolean = (BigDecimal(value) * 100.0) % 1 == 0

  def isInRange(value: Double): Boolean = value >= minValue && value <= maxValue

  def isInRangeWholeUnits(value: Double): Boolean =
    value >= payeWholeUnitsPaymentTypeMinValue && value <= payeWholeUnitsPaymentTypeMaxValue

  def isInRangePositiveWholeUnits(value: Double): Boolean =
    value >= payeWholeUnitsPositivePaymentTypeMinValue && value <= payeWholeUnitsPositivePaymentTypeMaxValue

  def paymentAmountValidator(implicit rds: Reads[Double]): Reads[Double] =
    verifying[Double](value => isInRange(value) && isMultipleOfPointZeroOne(value))

  def payeWholeUnitsPaymentTypeValidator(implicit rds: Reads[Int]): Reads[Int] =
    verifying[Int](value => isInRangeWholeUnits(value))

  def payeWholeUnitsPositivePaymentTypeValidator(implicit rds: Reads[Int]): Reads[Int] =
    verifying[Int](value => isInRangePositiveWholeUnits(value))

  implicit val incomePayeFormat: Format[IfPaye] = Format(
    (JsPath \ "paye").read[Seq[IfPayeEntry]].map(value => IfPaye(value)),
    (JsPath \ "paye").write[Seq[IfPayeEntry]].contramap(value => value.paye)
  )
}
