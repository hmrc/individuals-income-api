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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v1

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{times, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v1.SandboxSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.v1.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain.v1._
import uk.gov.hmrc.individualsincomeapi.services.v1.SandboxSaIncomeService
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.{failed, successful}

class SandboxSaIncomeControllerSpec extends SpecBase with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication().materializer

  val matchId: UUID = UUID.randomUUID()
  val utr: SaUtr = SaUtr("2432552644")
  val fromTaxYear: TaxYear = TaxYear("2018-19")
  val toTaxYear: TaxYear = TaxYear("2019-20")
  val taxYearInterval: TaxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
  val requestParameters =
    s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

  implicit val ec: ExecutionContext = ExecutionContext.global

  trait Setup {
    val mockSandboxSaIncomeService: SandboxSaIncomeService = mock[SandboxSaIncomeService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    val sandboxSaIncomeController =
      new SandboxSaIncomeController(mockSandboxSaIncomeService, mockAuthConnector, cc, mockAuditHelper)
  }

  "SandboxSaIncomeController.saFootprint" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
    val saFootprint = SaFootprint(
      registrations = Seq(SaRegistration(SaUtr("1234567890"), Some(LocalDate.parse("2014-05-01")))),
      taxReturns = Seq(
        SaTaxReturn(TaxYear("2015-16"), Seq(SaSubmission(SaUtr("1234567890"), Some(LocalDate.parse("2016-06-01")))))
      )
    )

    "return 200 (OK) with the registration information and self assessment returns for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result: Result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaFootprintPayload(requestParameters, saFootprint))
    }

    "return 200 (Ok) and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result: Result =
        await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaFootprintPayload(requestParametersWithoutToTaxYear, saFootprint))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "SandboxSaIncomeController.employmentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
    val employmentsIncomes = Seq(SaAnnualEmployments(TaxYear("2015-16"), Seq(SaEmploymentsIncome(utr, 9000))))

    "return 200 (OK) with the employments income returns for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result: Result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(employmentsIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result: Result =
        await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(employmentsIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "SandboxSaIncomeController.selfEmploymentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
    val selfEmploymentIncomes =
      Seq(SaAnnualSelfEmployments(TaxYear("2015-16"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 10500))))

    "return 200 (OK) with the self employments income for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(selfEmploymentIncomes))

      val result: Result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(selfEmploymentIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(selfEmploymentIncomes))

      val result: Result =
        await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(selfEmploymentIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "SandboxSaIncomeController.saReturnsSummary" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
    val taxReturnSummaries = Seq(SaTaxReturnSummaries(TaxYear("2015-16"), Seq(SaTaxReturnSummary(sandboxUtr, 20500))))

    "return 200 (OK) with the self tax return summaries for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(taxReturnSummaries))

      val result: Result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(taxReturnSummaries)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(taxReturnSummaries))

      val result: Result =
        await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(taxReturnSummaries))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "SandboxSaIncomeController.saTrustsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
    val saTrustIncomes = Seq(SaAnnualTrustIncomes(TaxYear("2015-16"), Seq(SaAnnualTrustIncome(sandboxUtr, 20500))))

    "return 200 (OK) with the self tax return trusts for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saTrustIncomes))

      val result: Result = await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saTrustIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saTrustIncomes))

      val result: Result =
        await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saTrustIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "SandboxSaIncomeController.saForeignIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
    val saForeignIncomes =
      Seq(SaAnnualForeignIncomes(TaxYear("2015-16"), Seq(SaAnnualForeignIncome(sandboxUtr, 1054.65))))

    "return 200 (OK) with the self tax return foreign income for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saForeignIncomes))

      val result: Result = await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saForeignIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saForeignIncomes))

      val result: Result =
        await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saForeignIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "SandboxSaIncomeController.saPartnershipsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")
    val saPartnershipIncomes =
      Seq(SaAnnualPartnershipIncomes(TaxYear("2015-16"), Seq(SaAnnualPartnershipIncome(sandboxUtr, 123.65))))

    "return 200 (OK) with the self tax return partnerships income for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saPartnershipIncomes))

      val result: Result = await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saPartnershipIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saPartnershipIncomes))

      val result: Result =
        await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saPartnershipIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchPartnershipsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "SandboxSaIncomeController.saPensionsAndStateBenefitsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
    val saPensionsAndStateBenefitsIncomes = Seq(
      SaAnnualPensionAndStateBenefitIncomes(
        TaxYear("2015-16"),
        Seq(SaAnnualPensionAndStateBenefitIncome(sandboxUtr, 123.65))
      )
    )

    "return 200 (OK) with the self tax return pensions and state benefits income for the period" in new Setup {
      `given`(
        mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(successful(saPensionsAndStateBenefitsIncomes))

      val result: Result =
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequest.uri, Json.toJson(saPensionsAndStateBenefitsIncomes))
      )
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParametersWithoutToTaxYear")

      `given`(
        mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(successful(saPensionsAndStateBenefitsIncomes))

      val result: Result = await(
        sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(
          fakeRequestWithoutToTaxYear
        )
      )

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saPensionsAndStateBenefitsIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(
        mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result =
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(
        mockSandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(failed(new Exception()))

      val result: Result =
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "SandboxSaIncomeController.saInterestsAndDividendsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParameters")
    val saInterestsAndDividendsIncomes = Seq(
      SaAnnualInterestAndDividendIncomes(
        TaxYear("2015-16"),
        Seq(SaAnnualInterestAndDividendIncome(sandboxUtr, 10.56, 56.34, 52.56))
      )
    )

    "return 200 (OK) with the self tax return interests and dividends income for the period" in new Setup {
      `given`(
        mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(successful(saInterestsAndDividendsIncomes))

      val result: Result =
        await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequest.uri, Json.toJson(saInterestsAndDividendsIncomes))
      )
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParametersWithoutToTaxYear")

      `given`(
        mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(successful(saInterestsAndDividendsIncomes))

      val result: Result = await(
        sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear)
      )

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saInterestsAndDividendsIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(
        mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result =
        await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(
        mockSandboxSaIncomeService.fetchInterestsAndDividendsIncome(refEq(matchId), refEq(taxYearInterval))(any())
      )
        .willReturn(failed(new Exception()))

      val result: Result =
        await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "SandboxSaIncomeController.saUkPropertiesIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
    val saUkPropertiesIncomes =
      Seq(SaAnnualUkPropertiesIncomes(TaxYear("2015-16"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 1276.67))))

    "return 200 (OK) with the UK properties income for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saUkPropertiesIncomes))

      val result: Result = await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saUkPropertiesIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saUkPropertiesIncomes))

      val result: Result =
        await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saUkPropertiesIncomes))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "SandboxSaIncomeController.saAdditionalInformation" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")
    val saAdditionalInformation = Seq(
      SaAnnualAdditionalInformations(TaxYear("2015-16"), Seq(SaAnnualAdditionalInformation(sandboxUtr, 76.67, 13.56)))
    )

    "return 200 (OK) with the additional information income for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saAdditionalInformation))

      val result: Result =
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saAdditionalInformation)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saAdditionalInformation))

      val result: Result =
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saAdditionalInformation))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result =
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result =
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "SandboxSaIncomeController.saOtherIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
    val otherIncome = Seq(SaAnnualOtherIncomes(TaxYear("2015-16"), Seq(SaAnnualOtherIncome(sandboxUtr, 26.70))))

    "return 200 (OK) with the other income for the period" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(otherIncome))

      val result: Result = await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(otherIncome)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParametersWithoutToTaxYear")

      `given`(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(otherIncome))

      val result: Result =
        await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(otherIncome))
      )
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 500 (Internal Server Error)" in new Setup {
      `given`(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  private def expectedSaFootprintPayload(requestParameters: String, saFootprint: SaFootprint) =
    s"""
       {
         "_links": {
           "self": {"href": "/individuals/income/sa?$requestParameters"},
           "additionalInformation": {"href": "/individuals/income/sa/additional-information?$requestParameters"},
           "employments": {"href": "/individuals/income/sa/employments?$requestParameters"},
           "foreign": {"href": "/individuals/income/sa/foreign?$requestParameters"},
           "interestsAndDividends": {"href": "/individuals/income/sa/interests-and-dividends?$requestParameters"},
           "other": {"href": "/individuals/income/sa/other?$requestParameters"},
           "partnerships": {"href": "/individuals/income/sa/partnerships?$requestParameters"},
           "pensionsAndStateBenefits": {"href": "/individuals/income/sa/pensions-and-state-benefits?$requestParameters"},
           "selfEmployments": {"href": "/individuals/income/sa/self-employments?$requestParameters"},
           "summary": {"href": "/individuals/income/sa/summary?$requestParameters"},
           "trusts": {"href": "/individuals/income/sa/trusts?$requestParameters"},
           "ukProperties": {"href": "/individuals/income/sa/uk-properties?$requestParameters"}
         },
         "selfAssessment": {
           "registrations": ${Json.toJson(saFootprint.registrations)},
           "taxReturns": ${Json.toJson(saFootprint.taxReturns)}
         }
       }
      """

  private def expectedSaPayload(fakeRequest: String, income: JsValue) =
    s"""
       {
         "_links": {
           "self": {"href": "$fakeRequest"}
         },
         "selfAssessment": {
           "taxReturns": $income
         }
       }
      """

}
