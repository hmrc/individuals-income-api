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
import org.scalatest.WordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.controllers.v2.SandboxSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.v2.SandboxSaIncomeService
import uk.gov.hmrc.individualsincomeapi.services.v2.ScopesService
import utils.{AuthHelper, SpecBase}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class SandboxSaIncomeControllerSpec extends WordSpec with AuthHelper with SpecBase with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val utr = SaUtr("2432552644")
  val fromTaxYear = TaxYear("2018-19")
  val toTaxYear = TaxYear("2019-20")
  val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
  val requestParameters =
    s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

  trait Setup {
    val mockSandboxSaIncomeService: SandboxSaIncomeService = mock[SandboxSaIncomeService]
    val mockAuthConnector = fakeAuthConnector(Future.successful(enrolments))
    lazy val scopeService: ScopesService = mock[ScopesService]
    val sandboxSaIncomeController =
      new SandboxSaIncomeController(mockSandboxSaIncomeService, scopeService, mockAuthConnector, cc)
    given(scopeService.getEndPointScopes(any())).willReturn(Seq("hello-world"))
  }

  "SandboxSaIncomeController.saFootprint" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
    val saFootprint = SaFootprint(
      registrations = Seq(SaRegistration(SaUtr("1234567890"), Some(LocalDate.parse("2014-05-01")))),
      taxReturns = Seq(
        SaTaxReturn(TaxYear("2015-16"), Seq(SaSubmission(SaUtr("1234567890"), Some(LocalDate.parse("2016-06-01"))))))
    )

    "return 500 with the registration information and self assessment returns for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saFootprint))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saFootprint))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

  }

  "SandboxSaIncomeController.employmentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
    val employmentsIncomes = Seq(SaAnnualEmployments(TaxYear("2015-16"), Seq(SaEmploymentsIncome(utr, 9000))))

    "return 500 with the employments income returns for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(employmentsIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(employmentsIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.selfEmploymentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
    val selfEmploymentIncomes =
      Seq(SaAnnualSelfEmployments(TaxYear("2015-16"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 10500))))

    "return 500 with the self employments income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(selfEmploymentIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(selfEmploymentIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saReturnsSummary" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
    val taxReturnSummaries = Seq(SaTaxReturnSummaries(TaxYear("2015-16"), Seq(SaTaxReturnSummary(sandboxUtr, 20500))))

    "return 500 with the self tax return summaries for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(taxReturnSummaries))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(taxReturnSummaries))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saTrustsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
    val saTrustIncomes = Seq(SaAnnualTrustIncomes(TaxYear("2015-16"), Seq(SaAnnualTrustIncome(sandboxUtr, 20500))))

    "return 500 with the self tax return trusts for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saTrustIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saTrustIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saForeignIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
    val saForeignIncomes =
      Seq(SaAnnualForeignIncomes(TaxYear("2015-16"), Seq(SaAnnualForeignIncome(sandboxUtr, 1054.65))))

    "return 500 with the self tax return foreign income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saForeignIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saForeignIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saPartnershipsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")
    val saPartnershipIncomes =
      Seq(SaAnnualPartnershipIncomes(TaxYear("2015-16"), Seq(SaAnnualPartnershipIncome(sandboxUtr, 123.65))))

    "return 500 with the self tax return partnerships income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saPartnershipIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saPartnershipIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saPensionsAndStateBenefitsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
    val saPensionsAndStateBenefitsIncomes = Seq(
      SaAnnualPensionAndStateBenefitIncomes(
        TaxYear("2015-16"),
        Seq(SaAnnualPensionAndStateBenefitIncome(sandboxUtr, 123.65))))

    "return 500 with the self tax return pensions and state benefits income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(
      //  mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saPensionsAndStateBenefitsIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(
      //  mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saPensionsAndStateBenefitsIncomes))

      val result = intercept[Exception] {
        await(
          sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(
            fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(
      //  mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saInterestsAndDividendsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParameters")
    val saInterestsAndDividendsIncomes = Seq(
      SaAnnualInterestAndDividendIncomes(
        TaxYear("2015-16"),
        Seq(SaAnnualInterestAndDividendIncome(sandboxUtr, 10.56, 56.34, 52.56))))

    "return 500 with the self tax return interests and dividends income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saInterestsAndDividendsIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saInterestsAndDividendsIncomes))

      val result = intercept[Exception] {
        await(
          sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(
            fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saUkPropertiesIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
    val saUkPropertiesIncomes =
      Seq(SaAnnualUkPropertiesIncomes(TaxYear("2015-16"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 1276.67))))

    "return 500 with the UK properties income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saUkPropertiesIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saUkPropertiesIncomes))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saAdditionalInformation" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")
    val saAdditionalInformation = Seq(
      SaAnnualAdditionalInformations(TaxYear("2015-16"), Seq(SaAnnualAdditionalInformation(sandboxUtr, 76.67, 13.56))))

    "return 500 with the additional information income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saAdditionalInformation))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(saAdditionalInformation))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }

  "SandboxSaIncomeController.saOtherIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
    val otherIncome = Seq(SaAnnualOtherIncomes(TaxYear("2015-16"), Seq(SaAnnualOtherIncome(sandboxUtr, 26.70))))

    "return 500 with the other income for the period" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(otherIncome))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParametersWithoutToTaxYear")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(successful(otherIncome))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] {
        await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))
      }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }
  }
}
