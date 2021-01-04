/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Reads.verifying
import play.api.libs.json.{Format, JsPath, Reads}

case class IfSa(sa: Seq[IfSaEntry])

object IfSa {

  val minValue = -9999999999.99
  val maxValue = 9999999999.99

  def isMultipleOfPointZeroOne(value: Double): Boolean = (BigDecimal(value) * 100.0) % 1 == 0

  def isInRange(value: Double): Boolean = value > minValue && value < maxValue

  def paymentAmountValidator(implicit rds: Reads[Double]): Reads[Double] =
    verifying[Double](value => isInRange(value) && isMultipleOfPointZeroOne(value))

  implicit val incomeSaFormat: Format[IfSa] = Format(
    (JsPath \ "sa").read[Seq[IfSaEntry]].map(value => IfSa(value)),
    (JsPath \ "sa").write[Seq[IfSaEntry]].contramap(value => value.sa)
  )

}
