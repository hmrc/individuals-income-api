/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.Matchers
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.play.test.UnitSpec

class TaxYearSpec extends UnitSpec with Matchers {

  val validTaxYears = Seq("2014-15", "2013-14", "2016-17", "2019-20", "2099-00")

  val invalidTaxYears = Seq("2014", "201314", "2016-1X", "A2014-15", "2015-17", "2013-18", "2015-14", "2015-15")

  "isValid" should {

    validTaxYears.foreach {
      taxYear => s"return true for tax year $taxYear" in {
          TaxYear.isValid(taxYear) shouldBe true
      }
    }

    invalidTaxYears.foreach {
      taxYear => s"return false for tax year $taxYear" in {
        TaxYear.isValid(taxYear) shouldBe false
      }
    }
  }

  "TaxYear constructor" should {
    validTaxYears.foreach {
      taxYear => s"create a taxYear for a valid argument '$taxYear'" in {
        TaxYear("2014-15").formattedTaxYear == taxYear
      }
    }

    invalidTaxYears.foreach {
      taxYear => s"throw an IllegalArgumentException for an invalid argument '$taxYear'" in {
        an [IllegalArgumentException] should be thrownBy TaxYear(taxYear)
      }
    }
  }
}
