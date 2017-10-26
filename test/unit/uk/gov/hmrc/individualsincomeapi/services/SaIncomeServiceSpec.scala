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

package unit.uk.gov.hmrc.individualsincomeapi.services

import java.util.UUID

import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.SandboxSaIncomeService
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.individualsincomeapi.util.Dates

class SaIncomeServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with Dates {

  val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")
  val fromTaxYear = TaxYear("2013-14")
  val toTaxYear = TaxYear("2016-17")
  val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)

  trait Setup {
    implicit val hc = HeaderCarrier()

    val sandboxSaIncomeService = new SandboxSaIncomeService()
  }

  "SandboxSaIncomeService.fetchSaReturnsByMatchId" should {
    "return the saReturns by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaReturnsByMatchId(sandboxMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualReturns(TaxYear("2014-15"), Seq(SaReturn(LocalDate.parse("2015-10-06")))),
        SaAnnualReturns(TaxYear("2013-14"), Seq(SaReturn(LocalDate.parse("2014-06-06"))))
      )
    }

    "filter out returns not in the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaReturnsByMatchId(sandboxMatchId, taxYearInterval.copy(toTaxYear = TaxYear("2013-14"))))

      result shouldBe Seq(
        SaAnnualReturns(TaxYear("2013-14"), Seq(SaReturn(LocalDate.parse("2014-06-06"))))
      )
    }

    "return an empty list when no annual return exists for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaReturnsByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))

      result shouldBe Seq()
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException]{
        await(sandboxSaIncomeService.fetchSaReturnsByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }
}
