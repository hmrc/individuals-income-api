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

package uk.gov.hmrc.individualsincomeapi.domain

import org.joda.time.{DateTime, DateTimeZone, LocalDate, MonthDay}
import play.api.libs.json._

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

case class TaxYear(formattedTaxYear: String) {
  if (!TaxYear.isValid(formattedTaxYear)) throw new IllegalArgumentException

  val startYr = formattedTaxYear.split("-")(0).toInt
  val endYr = startYr + 1
}

object TaxYear {

  implicit val formatTaxYear = new Format[TaxYear] {
    override def reads(json: JsValue): JsResult[TaxYear] = JsSuccess(TaxYear(json.asInstanceOf[JsString].value))

    override def writes(taxYear: TaxYear): JsValue = JsString(taxYear.formattedTaxYear)
  }

  private final val TaxYearRegex = "^(\\d{4})-(\\d{2})$"

  private final def firstDayOfTaxYear(year: Int): LocalDate = new MonthDay(4, 6).toLocalDate(year)

  private val matchTaxYear: String => Option[Match] = new Regex(TaxYearRegex, "first", "second") findFirstMatchIn _

  def fromEndYear(endYear: Int): TaxYear = TaxYear(s"%04d-%02d".format(endYear - 1, endYear % 100))

  def isValid(taxYearReference: String) = matchTaxYear(taxYearReference) exists { r =>
    (r.group("first").toInt + 1) % 100 == r.group("second").toInt
  }

  def current(): TaxYear = {
    val date = new LocalDate(DateTime.now(), DateTimeZone.forID("Europe/London"))
    if (date isBefore firstDayOfTaxYear(date.getYear)) fromEndYear(date.getYear)
    else fromEndYear(date.getYear + 1)
  }
}

case class TaxYearInterval(fromTaxYear: TaxYear, toTaxYear: TaxYear)
