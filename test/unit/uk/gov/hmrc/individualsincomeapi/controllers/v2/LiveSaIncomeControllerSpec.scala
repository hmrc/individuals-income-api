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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import scala.concurrent.*
import org.mockito.Mockito.when
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.*
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v2.SaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2._
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, TaxYear, TaxYearInterval}
import uk.gov.hmrc.individualsincomeapi.services.LiveCitizenMatchingService
import uk.gov.hmrc.individualsincomeapi.services.v2.{SaIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, IncomeSaHelpers, SpecBase, TestSupport}

import java.util.UUID
import scala.concurrent.Future.*
import scala.concurrent.{ExecutionContext, Future}

class LiveSaIncomeControllerSpec
    extends TestSupport with SpecBase with AuthHelper with MockitoSugar with IncomeSaHelpers {
  implicit lazy val materializer: Materializer = fakeApplication().materializer

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader: (String, String) = "CorrelationId" -> sampleCorrelationId

    val controllerComponent: ControllerComponents = fakeApplication().injector.instanceOf[ControllerComponents]
    val mockLiveSaIncomeService: SaIncomeService = mock[SaIncomeService]
    val mockLiveCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    implicit lazy val ec: ExecutionContext = fakeApplication().injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    val matchId: UUID = UUID.randomUUID()
    val matchIdString: String = matchId.toString
    val utr: SaUtr = SaUtr("2432552644")
    val fromTaxYear: TaxYear = TaxYear("2018-19")
    val toTaxYear: TaxYear = TaxYear("2019-20")
    val taxYearInterval: TaxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
    val requestParameters =
      s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

    val ifSa: Seq[IfSaEntry] = Seq(createValidSaTaxYearEntry())

    val saIncomeController =
      new SaIncomeController(
        mockLiveSaIncomeService,
        scopeService,
        scopesHelper,
        mockAuthConnector,
        controllerComponent,
        mockAuditHelper
      )

    implicit val hc: HeaderCarrier = HeaderCarrier()

    when(
      mockAuthConnector.authorise(
        eqTo(Enrolment("test-scope-1")),
        eqTo(Retrievals.allEnrolments)
      )(any(), any())
    ).thenReturn(
      Future.successful(Enrolments(Set(Enrolment("test-scope-1"))))
    )
  }

  "LiveSaIncomeController.saFootprint" should {

    "return 200 with the registration information and self assessment returns for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

      val saFootprint: Future[SaFootprint] = successful(SaFootprint.transform(ifSa))

      when(mockLiveSaIncomeService.fetchSaFootprint(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(saFootprint)

      val result: Result = await(
        saIncomeController.saFootprint(matchIdString, taxYearInterval)(
          fakeRequest.withHeaders(sampleCorrelationIdHeader)
        )
      )

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())

    }

    "return 200 and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSaFootprint(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFootprint.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saFootprint(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSaFootprint(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(saIncomeController.saFootprint(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

      when(mockLiveSaIncomeService.fetchSaFootprint(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFootprint.transform(ifSa)))

      val result: Result = await(saIncomeController.saFootprint(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchSaFootprint(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFootprint.transform(ifSa)))

      val result: Result = await(saIncomeController.saFootprint(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "LiveSaIncomeController.employmentsIncome" should {

    "return 200 with the employments income returns for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters").withHeaders(
          sampleCorrelationIdHeader
        )

      when(mockLiveSaIncomeService.fetchEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaEmployments.transform(ifSa)))

      val result: Result = await(
        saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(
          fakeRequest
        )
      )

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaEmployments.transform(ifSa)))

      val result: Result =
        await(saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")

      when(mockLiveSaIncomeService.fetchEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaEmployments.transform(ifSa)))

      val result: Result = await(saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/employments?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaEmployments.transform(ifSa)))

      val result: Result = await(saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saIncomeSource" should {

    "return 200 with the source income returns for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/source?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSources(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSources.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saIncomeSource(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/source?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "sources": [
           |          {
           |              "businessDescription": "This is a business description",
           |              "businessAddress": {
           |                  "line1": "line1",
           |                  "line2": "line2",
           |                  "line3": "line3",
           |                  "line4": "line4",
           |                  "postalCode": "QW123QW"
           |              },
           |              "telephoneNumber": "12345678901"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/source?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSources(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSources.transform(ifSa)))

      when(mockLiveSaIncomeService.fetchSources(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSources.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saIncomeSource(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/source?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "sources": [
           |          {
           |              "businessDescription": "This is a business description",
           |              "businessAddress": {
           |                  "line1": "line1",
           |                  "line2": "line2",
           |                  "line3": "line3",
           |                  "line4": "line4",
           |                  "postalCode": "QW123QW"
           |              },
           |              "telephoneNumber": "12345678901"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchSources(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saIncomeSource(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/source?$requestParameters")

      when(mockLiveSaIncomeService.fetchSources(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSources.transform(ifSa)))

      val result: Result = await(saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/source?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchSources(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSources.transform(ifSa)))

      val result: Result = await(saIncomeController.employmentsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.selfEmploymentsIncome" should {

    "return 200 with the self employments income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSelfEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSelfEmployments.transform(ifSa)))

      val result: Result = await(saIncomeController.selfEmploymentsIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSelfEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSelfEmployments.transform(ifSa)))

      val result: Result =
        await(saIncomeController.selfEmploymentsIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchSelfEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      when(mockLiveSaIncomeService.fetchSelfEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.selfEmploymentsIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")

      when(mockLiveSaIncomeService.fetchSelfEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSelfEmployments.transform(ifSa)))

      val result: Result = await(saIncomeController.selfEmploymentsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/self-employments?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchSelfEmployments(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSelfEmployments.transform(ifSa)))

      val result: Result = await(saIncomeController.selfEmploymentsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saReturnsSummary" should {

    "return 200 with the self tax return summaries for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSummary(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSummaries.transform(ifSa)))

      val result: Result = await(saIncomeController.saReturnsSummary(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchSummary(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSummaries.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saReturnsSummary(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchSummary(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saReturnsSummary(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns a bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")

      when(mockLiveSaIncomeService.fetchSummary(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSummaries.transform(ifSa)))

      val result: Result = await(saIncomeController.saReturnsSummary(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns a bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/summary?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchSummary(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaSummaries.transform(ifSa)))

      val result: Result = await(saIncomeController.saReturnsSummary(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saTrustsIncome" should {

    "return 200 with the self tax return trusts for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchTrusts(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaTrusts.transform(ifSa)))

      val result: Result = await(saIncomeController.saTrustsIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchTrusts(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaTrusts.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saTrustsIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchTrusts(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saTrustsIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct error message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")

      when(mockLiveSaIncomeService.fetchTrusts(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaTrusts.transform(ifSa)))

      val result: Result = await(saIncomeController.saTrustsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct error message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/trusts?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchTrusts(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaTrusts.transform(ifSa)))

      val result: Result = await(saIncomeController.saTrustsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saForeignIncome" should {

    "return 200 with the self tax return foreign income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchForeign(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaForeignIncomes.transform(ifSa)))

      val result: Result = await(saIncomeController.saForeignIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchForeign(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaForeignIncomes.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saForeignIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchForeign(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saForeignIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")

      when(mockLiveSaIncomeService.fetchForeign(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaForeignIncomes.transform(ifSa)))

      val result: Result = await(saIncomeController.saForeignIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/foreign?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchForeign(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaForeignIncomes.transform(ifSa)))

      val result: Result = await(saIncomeController.saForeignIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saPartnershipsIncome" should {

    "return 200 with the self tax return partnerships income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchPartnerships(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaPartnerships.transform(ifSa)))

      val result: Result = await(saIncomeController.saPartnershipsIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchPartnerships(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaPartnerships.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saPartnershipsIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchPartnerships(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saPartnershipsIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")

      when(mockLiveSaIncomeService.fetchPartnerships(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaPartnerships.transform(ifSa)))

      val result: Result = await(saIncomeController.saPartnershipsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/partnerships?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchPartnerships(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaPartnerships.transform(ifSa)))

      val result: Result = await(saIncomeController.saPartnershipsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saPensionsAndStateBenefitsIncome" should {

    "return 200 with the self tax return pensions and state benefits income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(
        mockLiveSaIncomeService.fetchPensionAndStateBenefits(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saPensionsAndStateBenefitsIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(
        mockLiveSaIncomeService.fetchPensionAndStateBenefits(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result: Result = await(
        saIncomeController.saPensionsAndStateBenefitsIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear)
      )

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(
        mockLiveSaIncomeService.fetchPensionAndStateBenefits(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result =
        await(
          saIncomeController.saPensionsAndStateBenefitsIncome(matchIdString, taxYearInterval)(
            FakeRequest().withHeaders(sampleCorrelationIdHeader)
          )
        )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")

      when(
        mockLiveSaIncomeService.fetchPensionAndStateBenefits(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saPensionsAndStateBenefitsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(
        mockLiveSaIncomeService.fetchPensionAndStateBenefits(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saPensionsAndStateBenefitsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

  }

  "LiveSaIncomeController.saInterestsAndDividendsIncome" should {

    "return 200 with the self tax return interests and dividends income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchInterestAndDividends(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaInterestAndDividends.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saInterestsAndDividendsIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/interests-and-dividends?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchInterestAndDividends(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaInterestAndDividends.transform(ifSa)))

      val result: Result = await(
        saIncomeController.saInterestsAndDividendsIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear)
      )

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchInterestAndDividends(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result =
        await(
          saIncomeController.saInterestsAndDividendsIncome(matchIdString, taxYearInterval)(
            FakeRequest().withHeaders(sampleCorrelationIdHeader)
          )
        )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")

      when(mockLiveSaIncomeService.fetchInterestAndDividends(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaInterestAndDividends.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saInterestsAndDividendsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/pensions-and-state-benefits?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchInterestAndDividends(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaInterestAndDividends.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saInterestsAndDividendsIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saUkPropertiesIncome" should {

    "return 200 with the UK properties income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchUkProperties(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaUkProperties.transform(ifSa)))

      val result: Result = await(saIncomeController.saUkPropertiesIncome(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchUkProperties(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaUkProperties.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saUkPropertiesIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchUkProperties(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saUkPropertiesIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")

      when(mockLiveSaIncomeService.fetchUkProperties(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaUkProperties.transform(ifSa)))

      val result: Result = await(saIncomeController.saUkPropertiesIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/uk-properties?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchUkProperties(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaUkProperties.transform(ifSa)))

      val result: Result = await(saIncomeController.saUkPropertiesIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saAdditionalInformation" should {

    "return 200 with the additional information income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(
        mockLiveSaIncomeService.fetchAdditionalInformation(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saAdditionalInformation(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(
        mockLiveSaIncomeService.fetchAdditionalInformation(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saAdditionalInformation(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(
        mockLiveSaIncomeService.fetchAdditionalInformation(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saAdditionalInformation(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")

      when(
        mockLiveSaIncomeService.fetchAdditionalInformation(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saAdditionalInformation(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/additional-information?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(
        mockLiveSaIncomeService.fetchAdditionalInformation(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any())
      )
        .thenReturn(Future.successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saAdditionalInformation(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saOtherIncome" should {

    "return 200 with the other income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchOtherIncome(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaOtherIncomeRecords.transform(ifSa)))

      val result: Result = await(saIncomeController.saOtherIncome(matchIdString, taxYearInterval)(fakeRequest))

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
           |            "otherIncome": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchOtherIncome(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaOtherIncomeRecords.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saOtherIncome(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
           |            "otherIncome": 100,
           |            "utr": "1234567890"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchOtherIncome(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saOtherIncome(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")

      when(mockLiveSaIncomeService.fetchOtherIncome(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaOtherIncomeRecords.transform(ifSa)))

      val result: Result = await(saIncomeController.saOtherIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/other?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchOtherIncome(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaOtherIncomeRecords.transform(ifSa)))

      val result: Result = await(saIncomeController.saOtherIncome(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }

  "LiveSaIncomeController.saFurtherDetails" should {

    "return 200 with the other income for the period" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParameters")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchFurtherDetails(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFurtherDetails.transform(ifSa)))

      val result: Result = await(saIncomeController.saFurtherDetails(matchIdString, taxYearInterval)(fakeRequest))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParametersWithoutToTaxYear")
          .withHeaders(sampleCorrelationIdHeader)

      when(mockLiveSaIncomeService.fetchFurtherDetails(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFurtherDetails.transform(ifSa)))

      val result: Result =
        await(saIncomeController.saFurtherDetails(matchIdString, taxYearInterval)(fakeRequestWithoutToTaxYear))

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

      verify(saIncomeController.auditHelper, times(1))
        .auditSaApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      when(mockLiveSaIncomeService.fetchFurtherDetails(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.failed(new MatchNotFoundException()))

      val result: Result = await(
        saIncomeController.saFurtherDetails(matchIdString, taxYearInterval)(
          FakeRequest().withHeaders(sampleCorrelationIdHeader)
        )
      )

      status(result) shouldBe NOT_FOUND

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParameters")

      when(mockLiveSaIncomeService.fetchFurtherDetails(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFurtherDetails.transform(ifSa)))

      val result: Result = await(saIncomeController.saFurtherDetails(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "returns bad request with correct message when malformed CorrelationId" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/sa/further-details?$requestParameters")
          .withHeaders("CorrelationId" -> "test")

      when(mockLiveSaIncomeService.fetchFurtherDetails(eqTo(matchId), eqTo(taxYearInterval), any())(any(), any()))
        .thenReturn(Future.successful(SaFurtherDetails.transform(ifSa)))

      val result: Result = await(saIncomeController.saFurtherDetails(matchIdString, taxYearInterval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(saIncomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }
}
