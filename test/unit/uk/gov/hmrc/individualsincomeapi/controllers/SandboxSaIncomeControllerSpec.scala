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

package unit.uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.SandboxSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.SandboxSaIncomeService
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future.{failed, successful}

class SandboxSaIncomeControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  implicit lazy val materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val utr = SaUtr("2432552644")
  val fromTaxYear = TaxYear("2015-16")
  val toTaxYear = TaxYear("2016-17")
  val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
  val requestParameters = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

  trait Setup {
    val mockAuthConnector = mock[ServiceAuthConnector]
    val mockSandboxSaIncomeService = mock[SandboxSaIncomeService]

    val sandboxSaIncomeController = new SandboxSaIncomeController(mockSandboxSaIncomeService, mockAuthConnector)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))
  }

  "SandboxSaIncomeController.saFootprint" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
    val saFootprint = SaFootprint(
      registrations = Seq(SaRegistration(SaUtr("1234567890"), LocalDate.parse("2014-05-01"))),
      taxReturns = Seq(SaTaxReturn(TaxYear("2015-16"), Seq(SaSubmission(SaUtr("1234567890"), LocalDate.parse("2016-06-01")))))
    )

    "return 200 (OK) with the registration information and self assessment returns for the period" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaFootprintByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaFootprintPayload(requestParameters, saFootprint))
    }

    "return 200 (Ok) and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSaFootprintByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaFootprintPayload(requestParametersWithoutToTaxYear, saFootprint))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaFootprintByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaFootprintByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }

  }

  "SandboxSaIncomeController.employmentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
    val employmentsIncomes = Seq(SaAnnualEmployments(TaxYear("2015-16"), Seq(SaEmploymentsIncome(utr, 9000))))

    "return 200 (OK) with the employments income returns for the period" in new Setup {
      given(mockSandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(employmentsIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(employmentsIncomes)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      given(mockSandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }

  "SandboxSaIncomeController.selfEmploymentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
    val selfEmploymentIncomes = Seq(SaAnnualSelfEmployments(TaxYear("2015-16"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 10500))))

    "return 200 (OK) with the self employments income for the period" in new Setup {
      given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(selfEmploymentIncomes))

      val result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(selfEmploymentIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(selfEmploymentIncomes))

      val result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(selfEmploymentIncomes)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      given(mockSandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(selfEmploymentIncomes))

      val result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }

  "SandboxSaIncomeService.saReturnsSummary" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
    val taxReturnSummaries = Seq(SaTaxReturnSummaries(TaxYear("2015-16"), Seq(SaTaxReturnSummary(sandboxUtr, 20500))))

    "return 200 (OK) with the self tax return summaries for the period" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(taxReturnSummaries))

      val result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(taxReturnSummaries)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(taxReturnSummaries))

      val result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(taxReturnSummaries)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(taxReturnSummaries))

      val result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }

  "SandboxSaIncomeService.saTrusts" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
    val saTrusts = Seq(SaAnnualTrusts(TaxYear("2015-16"), Seq(SaAnnualTrustIncome(sandboxUtr, 20500))))

    "return 200 (OK) with the self tax return trusts for the period" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaTrustsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saTrusts))

      val result = await(sandboxSaIncomeController.saTrusts(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saTrusts)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSaTrustsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saTrusts))

      val result = await(sandboxSaIncomeController.saTrusts(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saTrusts)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaTrustsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saTrusts(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaTrustsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saTrusts))

      val result = await(sandboxSaIncomeController.saTrusts(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }
  }

  private def expectedSaFootprintPayload(requestParameters: String, saFootprint: SaFootprint) = {
    s"""
       {
         "_links": {
           "self": {"href": "/individuals/income/sa?$requestParameters"},
           "employments": {"href": "/individuals/income/sa/employments?$requestParameters"},
           "selfEmployments": {"href": "/individuals/income/sa/self-employments?$requestParameters"},
           "summary": {"href": "/individuals/income/sa/summary?$requestParameters"}
         },
         "selfAssessment": {
           "registrations": ${Json.toJson(saFootprint.registrations)},
           "taxReturns": ${Json.toJson(saFootprint.taxReturns)}
         }
       }
      """
  }

  private def expectedSaPayload(fakeRequest: String, income: JsValue) = {
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

}
