/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq, eq => eqTo}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{times, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v2.LiveSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain.v2._
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, TaxYear, TaxYearInterval}
import uk.gov.hmrc.individualsincomeapi.services.LiveCitizenMatchingService
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveSaIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, IncomeSaHelpers, SpecBase, TestSupport}

import java.util.UUID
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class LiveSaIncomeControllerSpec
    extends TestSupport with SpecBase with AuthHelper with MockitoSugar with IncomeSaHelpers {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader = ("CorrelationId" -> sampleCorrelationId)

    val controllerComponent = fakeApplication.injector.instanceOf[ControllerComponents]
    val mockLiveSaIncomeService = mock[LiveSaIncomeService]
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper : AuditHelper = mock[AuditHelper]

    val matchId = UUID.randomUUID()
    val utr = SaUtr("2432552644")
    val fromTaxYear = TaxYear("2018-19")
    val toTaxYear = TaxYear("2019-20")
    val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
    val requestParameters =
      s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

    val ifSa = Seq(createValidSaTaxYearEntry())

    val liveSaIncomeController =
      new LiveSaIncomeController(
        mockLiveSaIncomeService,
        scopeService,
        scopesHelper,
        mockAuthConnector,
        controllerComponent,
        mockAuditHelper)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockAuthConnector.authorise(eqTo(Enrolment("test-scope-1")), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope-1")))))
  }

  "LiveSaIncomeController.saFootprint" should {

    "return 200 with the registration information and self assessment returns for the period" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFootprint.transform(ifSa)))

      val result = await(
        liveSaIncomeController.saFootprint(matchId, taxYearInterval)(
          fakeRequest.withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "ukProperties": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's uk-properties sa data"
           |    },
           |    "trusts": {
           |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa trusts data"
           |    },
           |    "selfEmployments": {
           |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's self-employments sa data"
           |    },
           |    "partnerships": {
           |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa partnerships data"
           |    },
           |    "self": {
           |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    },
           |    "interestsAndDividends": {
           |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's interests-and-dividends sa data"
           |    },
           |    "furtherDetails": {
           |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's further-details sa data"
           |    },
           |    "additionalInformation": {
           |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's additional-information sa data"
           |    },
           |    "other": {
           |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's other sa data"
           |    },
           |    "foreign": {
           |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa foreign data"
           |    },
           |    "summary": {
           |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa summary data"
           |    },
           |    "employments": {
           |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's employments sa data"
           |    },
           |    "pensionsAndStateBenefits": {
           |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's pensions-and-state-benefits sa data"
           |    },
           |    "source": {
           |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's source sa data"
           |    }
           |  },
           |  "selfAssessment": {
           |    "registrations": [
           |      {
           |        "registrationDate": "2020-01-01",
           |        "utr": "1234567890"
           |      }
           |    ],
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "submissions": [
           |          {
           |            "receivedDate": "2020-01-01",
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())

    }

    "return 200 and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")
        .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFootprint.transform(ifSa)))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "ukProperties": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's uk-properties sa data"
           |    },
           |    "trusts": {
           |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa trusts data"
           |    },
           |    "selfEmployments": {
           |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's self-employments sa data"
           |    },
           |    "partnerships": {
           |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa partnerships data"
           |    },
           |    "self": {
           |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2018-19"
           |    },
           |    "interestsAndDividends": {
           |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's interests-and-dividends sa data"
           |    },
           |    "furtherDetails": {
           |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's further-details sa data"
           |    },
           |    "additionalInformation": {
           |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's additional-information sa data"
           |    },
           |    "other": {
           |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's other sa data"
           |    },
           |    "foreign": {
           |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa foreign data"
           |    },
           |    "summary": {
           |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa summary data"
           |    },
           |    "employments": {
           |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's employments sa data"
           |    },
           |    "pensionsAndStateBenefits": {
           |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's pensions-and-state-benefits sa data"
           |    },
           |    "source": {
           |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's source sa data"
           |    }
           |  },
           |  "selfAssessment": {
           |    "registrations": [
           |      {
           |        "registrationDate": "2020-01-01",
           |        "utr": "1234567890"
           |      }
           |    ],
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "submissions": [
           |          {
           |            "receivedDate": "2020-01-01",
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
        .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFootprint.transform(ifSa)))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))


      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFootprint.transform(ifSa)))

      val result = await(liveSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "LiveSaIncomeController.employmentsIncome" should {

    "return 200 with the employments income returns for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaEmployments.transform(ifSa)))

      val result =
        await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "employments": [
           |          {
           |            "employmentIncome": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaEmployments.transform(ifSa)))

      val result =
        await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "employments": [
           |          {
           |            "employmentIncome": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")

      given(mockLiveSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaEmployments.transform(ifSa)))

      val result = await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaEmployments.transform(ifSa)))

      val result = await(liveSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.selfEmploymentsIncome" should {

    "return 200 with the self employments income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSelfEmployments.transform(ifSa)))

      val result = await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "selfEmployments": [
           |          {
           |            "selfEmploymentProfit": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSelfEmployments.transform(ifSa)))

      val result =
        await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "selfEmployments": [
           |          {
           |            "selfEmploymentProfit": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")

      given(mockLiveSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSelfEmployments.transform(ifSa)))

      val result = await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSelfEmployments.transform(ifSa)))

      val result = await(liveSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saReturnsSummary" should {

    "return 200 with the self tax return summaries for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSummaries.transform(ifSa)))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "summary": [
           |          {
           |            "totalIncome": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSummaries.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "summary": [
           |          {
           |            "totalIncome": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns a bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")

      given(mockLiveSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSummaries.transform(ifSa)))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns a bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaSummaries.transform(ifSa)))

      val result = await(liveSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saTrustsIncome" should {

    "return 200 with the self tax return trusts for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaTrusts.transform(ifSa)))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "trusts": [
           |          {
           |            "trustIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaTrusts.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "trusts": [
           |          {
           |            "trustIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct error message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")

      given(mockLiveSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaTrusts.transform(ifSa)))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct error message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaTrusts.transform(ifSa)))

      val result = await(liveSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saForeignIncome" should {

    "return 200 with the self tax return foreign income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaForeignIncomes.transform(ifSa)))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "foreign": [
           |          {
           |            "foreignIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaForeignIncomes.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "foreign": [
           |          {
           |            "foreignIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")

      given(mockLiveSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaForeignIncomes.transform(ifSa)))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaForeignIncomes.transform(ifSa)))

      val result = await(liveSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saPartnershipsIncome" should {

    "return 200 with the self tax return partnerships income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPartnerships.transform(ifSa)))

      val result = await(liveSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "partnerships": [
           |          {
           |            "partnershipProfit": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPartnerships.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "partnerships": [
           |          {
           |            "partnershipProfit": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")

      given(mockLiveSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPartnerships.transform(ifSa)))

      val result = await(liveSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPartnerships.transform(ifSa)))

      val result = await(liveSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saPensionsAndStateBenefitsIncome" should {

    "return 200 with the self tax return pensions and state benefits income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(
        mockLiveSaIncomeService
          .fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "pensionsAndStateBenefits": [
           |          {
           |            "totalIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(
        mockLiveSaIncomeService
          .fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result = await(
        liveSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "pensionsAndStateBenefits": [
           |          {
           |            "totalIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {
      given(
        mockLiveSaIncomeService
          .fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result =
        await(
          liveSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(
            FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")

      given(mockLiveSaIncomeService.fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result = await(liveSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result = await(liveSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "LiveSaIncomeController.saInterestsAndDividendsIncome" should {

    "return 200 with the self tax return interests and dividends income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(
        mockLiveSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaInterestAndDividends.transform(ifSa)))

      val result = await(liveSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "interestsAndDividends": [
           |          {
           |            "ukInterestsIncome": 100,
           |            "foreignDividendsIncome": 100,
           |            "ukDividendsIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(
        mockLiveSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaInterestAndDividends.transform(ifSa)))

      val result = await(
        liveSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "interestsAndDividends": [
           |          {
           |            "ukInterestsIncome": 100,
           |            "foreignDividendsIncome": 100,
           |            "ukDividendsIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(
        mockLiveSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result =
        await(
          liveSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(
            FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")

      given(mockLiveSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaInterestAndDividends.transform(ifSa)))

      val result = await(liveSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaInterestAndDividends.transform(ifSa)))

      val result = await(liveSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saUkPropertiesIncome" should {

    "return 200 with the UK properties income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaUkProperties.transform(ifSa)))

      val result = await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "ukProperties": [
           |          {
           |            "totalProfit": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaUkProperties.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "ukProperties": [
           |          {
           |            "totalProfit": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")

      given(mockLiveSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaUkProperties.transform(ifSa)))

      val result = await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaUkProperties.transform(ifSa)))

      val result = await(liveSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saAdditionalInformation" should {

    "return 200 with the additional information income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(
        mockLiveSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result = await(liveSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "additionalInformation": [
           |          {
           |            "gainsOnLifePolicies": 100,
           |            "sharesOptionsIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(
        mockLiveSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "additionalInformation": [
           |          {
           |            "gainsOnLifePolicies": 100,
           |            "sharesOptionsIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(
        mockLiveSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")

      given(mockLiveSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result = await(liveSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result = await(liveSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saOtherIncome" should {

    "return 200 with the other income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaOtherIncomeRecords.transform(ifSa)))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "other": [
           |          {
           |            "otherIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaOtherIncomeRecords.transform(ifSa)))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "other": [
           |          {
           |            "otherIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaOtherIncomeRecords.transform(ifSa)))

      val result = await(liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with corrrect message when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaOtherIncomeRecords.transform(ifSa)))

      val result = await((liveSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest)))

        status(result) shouldBe BAD_REQUEST
        jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saFurtherDetails" should {

    "return 200 with the other income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFurtherDetails.transform(ifSa)))

      val result = await(liveSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/further-details?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "furtherDetails": [
           |          {
           |            "busStartDate": "2020-01-01",
           |            "busEndDate": "2020-01-30",
           |            "totalTaxPaid": 100.01,
           |            "totalNIC": 100.01,
           |            "turnover": 100.01,
           |            "otherBusIncome": 100.01,
           |            "tradingIncomeAllowance": 100.01,
           |            "deducts": {
           |              "totalBusExpenses": 200,
           |              "totalDisallowBusExp": 200
           |            }
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      given(mockLiveSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFurtherDetails.transform(ifSa)))

      val result =
        await(liveSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/further-details?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "furtherDetails": [
           |          {
           |            "busStartDate": "2020-01-01",
           |            "busEndDate": "2020-01-30",
           |            "totalTaxPaid": 100.01,
           |            "totalNIC": 100.01,
           |            "turnover": 100.01,
           |            "otherBusIncome": 100.01,
           |            "tradingIncomeAllowance": 100.01,
           |            "deducts": {
           |              "totalBusExpenses": 200,
           |              "totalDisallowBusExp": 200
           |            }
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(
        liveSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParameters")

      given(mockLiveSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFurtherDetails.transform(ifSa)))

      val result = await(liveSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct messagfe when malformed CorrelationId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParameters")
        .withHeaders("CorrelationId" -> "test")

      given(mockLiveSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any(), any()))
        .willReturn(successful(SaFurtherDetails.transform(ifSa)))

      val result = await(liveSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveSaIncomeController.auditHelper, times(1)).
        auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }
}
