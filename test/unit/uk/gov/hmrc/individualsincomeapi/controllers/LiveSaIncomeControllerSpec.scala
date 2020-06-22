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

package unit.uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import akka.stream.Materializer
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verifyNoInteractions
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.controllers.LiveSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.LiveSaIncomeService
import utils.SpecBase

import scala.concurrent.Future.{failed, successful}

class LiveSaIncomeControllerSpec extends SpecBase with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val utr = SaUtr("2432552644")
  val fromTaxYear = TaxYear("2018-19")
  val toTaxYear = TaxYear("2019-20")
  val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
  val requestParameters =
    s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

  trait Setup {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockLiveSaIncomeService: LiveSaIncomeService = mock[LiveSaIncomeService]

    val liveSaIncomeController = new LiveSaIncomeController(mockLiveSaIncomeService, mockAuthConnector, cc)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))
  }

  "LiveSaIncomeController.saFootprint" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
    val saFootprint = SaFootprint(
      Seq(SaRegistration(utr, Some(LocalDate.parse("2018-06-01")))),
      Seq(SaTaxReturn(TaxYear("2018-19"), Seq(SaSubmission(utr, Some(LocalDate.parse("2019-06-01"))))))
    )

    "return 200 (OK) with the self assessment footprint for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaFootprintPayload(requestParameters, saFootprint))
    }

    "return 200 (Ok) and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saFootprint))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaFootprintPayload(requestParametersWithoutToTaxYear, saFootprint))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.saReturnsSummary" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
    val saReturnSummaries = Seq(SaTaxReturnSummaries(TaxYear("2018-19"), Seq(SaTaxReturnSummary(utr, 30500.55))))

    "return 200 (OK) when there are sa tax returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturnSummaries))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saReturnSummaries)))
    }

    "return 200 (Ok) and a link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturnSummaries))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saReturnSummaries)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchReturnsSummary(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-summary privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa-summary")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.employmentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
    val employmentsIncomes = Seq(SaAnnualEmployments(TaxYear("2018-19"), Seq(SaEmploymentsIncome(utr, 9000))))

    "return 200 (OK) with the employments income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result = await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(employmentsIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(employmentsIncomes))

      val result =
        await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(employmentsIncomes)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-employments privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa-employments")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.selfEmploymentsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
    val saIncomes = Seq(SaAnnualSelfEmployments(TaxYear("2018-19"), Seq(SaSelfEmploymentsIncome(utr, 9000.55))))

    "return 200 (OK) with the employments income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result =
        await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saIncomes)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchSelfEmploymentsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-self-employments privileged scope" in new Setup {
      given(
        mockAuthConnector.authorise(
          refEq(Enrolment("read:individuals-income-sa-self-employments")),
          refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.saTrustsIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
    val saIncomes = Seq(SaAnnualTrustIncomes(TaxYear("2018-19"), Seq(SaAnnualTrustIncome(utr, 9000.55))))

    "return 200 (OK) with the trusts income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saIncomes)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saIncomes))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saIncomes)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchTrustsIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-trusts privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa-trusts")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.saForeignIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
    val saForeignIncome =
      Seq(SaAnnualForeignIncomes(TaxYear("2018-19"), Seq(SaAnnualForeignIncome(sandboxUtr, 1054.65))))

    "return 200 (OK) with the foreign income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saForeignIncome))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saForeignIncome)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saForeignIncome))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saForeignIncome)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchForeignIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-foreign privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa-foreign")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.saUkPropertiesIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
    val saUkPropertiesIncome =
      Seq(SaAnnualUkPropertiesIncomes(TaxYear("2018-19"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 1276.67))))

    "return 200 (OK) with the uk properties income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saUkPropertiesIncome))

      val result = await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saUkPropertiesIncome)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saUkPropertiesIncome))

      val result =
        await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saUkPropertiesIncome)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchUkPropertiesIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-uk-properties privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa-uk-properties")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
    }
  }

  "LiveSaIncomeController.saOtherIncome" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
    val saOtherIncome = Seq(SaAnnualOtherIncomes(TaxYear("2018-19"), Seq(SaAnnualOtherIncome(sandboxUtr, 134.56))))

    "return 200 (OK) with the other income returns for the period" in new Setup {
      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saOtherIncome))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedSaPayload(fakeRequest.uri, Json.toJson(saOtherIncome)))
    }

    "return 200 (Ok) and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParametersWithoutToTaxYear")

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saOtherIncome))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(
        expectedSaPayload(fakeRequestWithoutToTaxYear.uri, Json.toJson(saOtherIncome)))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "require read:individuals-income-sa-other privileged scope" in new Setup {
      given(
        mockAuthConnector
          .authorise(refEq(Enrolment("read:individuals-income-sa-other")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveSaIncomeService)
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
