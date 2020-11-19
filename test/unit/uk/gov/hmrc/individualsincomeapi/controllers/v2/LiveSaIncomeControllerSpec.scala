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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import java.util.UUID

import akka.stream.Materializer
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.controllers.v2.LiveSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.{LiveSaIncomeService, ScopesService}
import utils.{AuthHelper, SpecBase}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class LiveSaIncomeControllerSpec extends SpecBase with AuthHelper with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val utr = SaUtr("2432552644")
  val fromTaxYear = TaxYear("2018-19")
  val toTaxYear = TaxYear("2019-20")
  val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
  val requestParameters =
    s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

  trait Setup {
    val mockAuthConnector: AuthConnector = fakeAuthConnector(Future.successful(enrolments))
    val mockLiveSaIncomeService: LiveSaIncomeService = mock[LiveSaIncomeService]
    lazy val scopeService: ScopesService = mock[ScopesService]

    val liveSaIncomeController =
      new LiveSaIncomeController(mockLiveSaIncomeService, scopeService, mockAuthConnector, cc)

    given(scopeService.getEndPointScopes(any())).willReturn(Seq("hello-world"))
  }

  "LiveSaIncomeController.saFootprint" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
    val saFootprint = SaFootprint(
      Seq(SaRegistration(utr, Some(LocalDate.parse("2018-06-01")))),
      Seq(SaTaxReturn(TaxYear("2018-19"), Seq(SaSubmission(utr, Some(LocalDate.parse("2019-06-01"))))))
    )

    "return 500 with the self assessment footprint for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "LiveSaIncomeController.saReturnsSummary" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
    val saReturnSummaries = Seq(SaTaxReturnSummaries(TaxYear("2018-19"), Seq(SaTaxReturnSummary(utr, 30500.55))))

    "return 500 when there are sa tax returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturnSummaries))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and a link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturnSummaries))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "LiveSaIncomeController.employmentsIncome" should {

    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
    val employmentsIncomes = Seq(SaAnnualEmployments(TaxYear("2018-19"), Seq(SaEmploymentsIncome(utr, 9000))))

    "return 500 with the employments income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result = intercept[Exception] {
        await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result = intercept[Exception] {
        await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

  }

  "LiveSaIncomeController.selfEmploymentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
    val saIncomes = Seq(SaAnnualSelfEmployments(TaxYear("2018-19"), Seq(SaSelfEmploymentsIncome(utr, 9000.55))))

    "return 500 with the employments income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = intercept[Exception] {
        await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = intercept[Exception] {
        await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "LiveSaIncomeController.saTrustsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
    val saIncomes = Seq(SaAnnualTrustIncomes(TaxYear("2018-19"), Seq(SaAnnualTrustIncome(utr, 9000.55))))

    "return 500 with the trusts income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "LiveSaIncomeController.saForeignIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
    val saForeignIncome =
      Seq(SaAnnualForeignIncomes(TaxYear("2018-19"), Seq(SaAnnualForeignIncome(sandboxUtr, 1054.65))))

    "return 500 with the foreign income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saForeignIncome))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saForeignIncome))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "LiveSaIncomeController.saUkPropertiesIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
    val saUkPropertiesIncome =
      Seq(SaAnnualUkPropertiesIncomes(TaxYear("2018-19"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 1276.67))))

    "return 500 with the uk properties income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saUkPropertiesIncome))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saUkPropertiesIncome))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "LiveSaIncomeController.saOtherIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
    val saOtherIncome = Seq(SaAnnualOtherIncomes(TaxYear("2018-19"), Seq(SaAnnualOtherIncome(sandboxUtr, 134.56))))

    "return 500 with the other income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saOtherIncome))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saOtherIncome))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }
}
