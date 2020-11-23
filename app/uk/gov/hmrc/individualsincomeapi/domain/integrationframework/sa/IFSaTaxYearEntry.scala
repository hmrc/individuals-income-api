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
import play.api.libs.json.Reads.pattern
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa.IFIncomeSa.{paymentAmountValidator}

case class IFSaTaxYearEntry(
  taxYear: Option[String],
  income: Option[Double],
  returnList: Option[Seq[IFSaReturnType]]
)

object IFSaTaxYearEntry {

  val taxYearPattern = "^20[0-9]{2}$".r

  implicit val saTaxYearEntryFormat: Format[IFSaTaxYearEntry] = Format(
    (
      (JsPath \ "taxYear").readNullable[String](pattern(taxYearPattern, "Invalid Tax Year")) and
        (JsPath \ "income").readNullable[Double](paymentAmountValidator) and
        (JsPath \ "returnList").readNullable[Seq[IFSaReturnType]]
    )(IFSaTaxYearEntry.apply _),
    (
      (JsPath \ "taxYear").writeNullable[String] and
        (JsPath \ "income").writeNullable[Double] and
        (JsPath \ "returnList").writeNullable[Seq[IFSaReturnType]]
    )(unlift(IFSaTaxYearEntry.unapply))
  )

}
