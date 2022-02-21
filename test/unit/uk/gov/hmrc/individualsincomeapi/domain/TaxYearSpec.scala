/*
 * Copyright 2022 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsincomeapi.domain

import org.joda.time.{DateTimeUtils, LocalDate}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import utils.TestSupport

class TaxYearSpec extends TestSupport with BeforeAndAfterEach {

  val validTaxYears = Seq("2014-15", "2013-14", "2016-17", "2019-20", "2099-00")

  val invalidTaxYears = Seq("2014", "201314", "2016-1X", "A2014-15", "2015-17", "2013-18", "2015-14", "2015-15")

  override def afterEach: Unit =
    DateTimeUtils.setCurrentMillisSystem()

  "isValid" should {

    validTaxYears.foreach { taxYear =>
      s"return true for tax year $taxYear" in {
        TaxYear.isValid(taxYear) shouldBe true
      }
    }

    invalidTaxYears.foreach { taxYear =>
      s"return false for tax year $taxYear" in {
        TaxYear.isValid(taxYear) shouldBe false
      }
    }
  }

  "TaxYear constructor" should {
    validTaxYears.foreach { taxYear =>
      s"create a taxYear for a valid argument '$taxYear'" in {
        TaxYear(taxYear).formattedTaxYear == taxYear shouldBe true
      }
    }

    invalidTaxYears.foreach { taxYear =>
      s"throw an IllegalArgumentException for an invalid argument '$taxYear'" in {
        an[IllegalArgumentException] should be thrownBy TaxYear(taxYear)
      }
    }
  }

  "fromEndYear" should {
    "return the correct tax year from an end year" in {
      TaxYear.fromEndYear(2017) shouldBe TaxYear("2016-17")
      TaxYear.fromEndYear(2009) shouldBe TaxYear("2008-09")
      TaxYear.fromEndYear(2000) shouldBe TaxYear("1999-00")
      TaxYear.fromEndYear(1999) shouldBe TaxYear("1998-99")
    }
  }

  "TaxYear.current()" should {
    "return tax year 2015-16 when date is before 2016-04-05" in {
      DateTimeUtils.setCurrentMillisFixed(LocalDate.parse("2016-04-05").toDate.getTime)

      TaxYear.current() shouldBe TaxYear("2015-16")
    }

    "return tax year 2016-17 when date is after 2016-04-06" in {
      DateTimeUtils.setCurrentMillisFixed(LocalDate.parse("2016-04-06").toDate.getTime)

      TaxYear.current() shouldBe TaxYear("2016-17")
    }
  }

  "Json formats" should {
    "read correctly from Json" in {
      val result = Json.fromJson[TaxYear](JsString("2019-20"))
      result.get shouldBe TaxYear("2019-20")
    }
  }

}
