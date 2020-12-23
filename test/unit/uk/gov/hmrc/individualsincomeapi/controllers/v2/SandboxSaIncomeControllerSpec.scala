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
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.controllers.v2.SandboxSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.v2.{SaAdditionalInformationRecords, SaEmployments, SaFootprint, SaForeignIncomes, SaFurtherDetails, SaInterestAndDividends, SaOtherIncomeRecords, SaPartnerships, SaPensionAndStateBenefits, SaSelfEmployments, SaSummaries, SaTrusts, SaUkProperties}
import uk.gov.hmrc.individualsincomeapi.services.v2.{SandboxSaIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, IncomeSaHelpers, SpecBase, TestSupport}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import play.api.http.Status._
import uk.gov.hmrc.individualsincomeapi.services.SandboxCitizenMatchingService

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.{failed, successful}

class SandboxSaIncomeControllerSpec
    extends TestSupport with SpecBase with AuthHelper with MockitoSugar with IncomeSaHelpers {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup extends ScopesConfigHelper {

    val controllerComponent = fakeApplication.injector.instanceOf[ControllerComponents]
    val mockSandboxSaIncomeService = mock[SandboxSaIncomeService]
    val mockSandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val matchId = UUID.randomUUID()
    val utr = SaUtr("2432552644")
    val fromTaxYear = TaxYear("2018-19")
    val toTaxYear = TaxYear("2019-20")
    val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
    val requestParameters =
      s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"

    val ifSa = Seq(createValidSaTaxYearEntry())

    val sandboxSaIncomeController =
      new SandboxSaIncomeController(
        mockSandboxSaIncomeService,
        scopeService,
        scopesHelper,
        mockAuthConnector,
        controllerComponent)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockAuthConnector.authorise(eqTo(Enrolment("test-scope-1")), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope-1")))))
  }

  "SandboxSaIncomeController.saFootprint" should {

    "return 200 with the registration information and self assessment returns for the period" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

      given(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaFootprint.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "incomeSaUkProperties": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's uk-properties sa data"
           |    },
           |    "incomeSaTrusts": {
           |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa trusts data"
           |    },
           |    "incomeSaSelfEmployments": {
           |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's self-employments sa data"
           |    },
           |    "incomeSaPartnerships": {
           |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa partnerships data"
           |    },
           |    "self": {
           |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    },
           |    "incomeSaInterestsAndDividends": {
           |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's interests-and-dividends sa data"
           |    },
           |    "incomeSaFurtherDetails": {
           |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's further-details sa data"
           |    },
           |    "incomeSaAdditionalInformation": {
           |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's additional-information sa data"
           |    },
           |    "incomeSaOther": {
           |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's other sa data"
           |    },
           |    "incomeSaForeign": {
           |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa foreign data"
           |    },
           |    "incomeSaSummary": {
           |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa summary data"
           |    },
           |    "incomeSaEmployments": {
           |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's employments sa data"
           |    },
           |    "incomeSaPensionsAndStateBenefits": {
           |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's pensions-and-state-benefits sa data"
           |    },
           |    "incomeSaSource": {
           |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's source sa data"
           |    }
           |  },
           |  "selfAssessment": {
           |    "registrations": [
           |      {
           |        "registrationDate": "2020-01-01"
           |      }
           |    ],
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "submissions": [
           |          {
           |            "receivedDate": "2020-01-01"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

    }

    "return 200 and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaFootprint.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "incomeSaUkProperties": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's uk-properties sa data"
           |    },
           |    "incomeSaTrusts": {
           |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa trusts data"
           |    },
           |    "incomeSaSelfEmployments": {
           |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's self-employments sa data"
           |    },
           |    "incomeSaPartnerships": {
           |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa partnerships data"
           |    },
           |    "self": {
           |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2018-19"
           |    },
           |    "incomeSaInterestsAndDividends": {
           |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's interests-and-dividends sa data"
           |    },
           |    "incomeSaFurtherDetails": {
           |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's further-details sa data"
           |    },
           |    "incomeSaAdditionalInformation": {
           |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's additional-information sa data"
           |    },
           |    "incomeSaOther": {
           |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's other sa data"
           |    },
           |    "incomeSaForeign": {
           |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa foreign data"
           |    },
           |    "incomeSaSummary": {
           |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's income sa summary data"
           |    },
           |    "incomeSaEmployments": {
           |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's employments sa data"
           |    },
           |    "incomeSaPensionsAndStateBenefits": {
           |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's pensions-and-state-benefits sa data"
           |    },
           |    "incomeSaSource": {
           |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
           |      "title": "Get an individual's source sa data"
           |    }
           |  },
           |  "selfAssessment": {
           |    "registrations": [
           |      {
           |        "registrationDate": "2020-01-01"
           |      }
           |    ],
           |    "taxReturns": [
           |      {
           |        "taxYear": "2019-20",
           |        "submissions": [
           |          {
           |            "receivedDate": "2020-01-01"
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 404 for an invalid matchId" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

      given(mockSandboxSaIncomeService.fetchSaFootprint(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saFootprint(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
    }

  }

  "SandboxSaIncomeController.employmentsIncome" should {

    "return 200 with the employments income returns for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/employments?$requestParameters")

      given(mockSandboxSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaEmployments.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequest))

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
           |            "employmentIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/employments?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaEmployments.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
           |            "employmentIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchEmployments(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.employmentsIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.selfEmploymentsIncome" should {

    "return 200 with the self employments income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/self-employments?$requestParameters")

      given(mockSandboxSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaSelfEmployments.transform(ifSa)))

      val result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequest))

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
           |            "selfEmploymentProfit": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/self-employments?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaSelfEmployments.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
           |            "selfEmploymentProfit": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchSelfEmployments(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.selfEmploymentsIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saReturnsSummary" should {

    "return 200 with the self tax return summaries for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/summary?$requestParameters")

      given(mockSandboxSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaSummaries.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequest))

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
           |            "totalIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/summary?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaSummaries.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
           |            "totalIncome": 100
           |          }
           |        ]
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchSummary(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saReturnsSummary(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saTrustsIncome" should {

    "return 200 with the self tax return trusts for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/trusts?$requestParameters")

      given(mockSandboxSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaTrusts.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/trusts?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaTrusts.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchTrusts(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saTrustsIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saForeignIncome" should {

    "return 200 with the self tax return foreign income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/foreign?$requestParameters")

      given(mockSandboxSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaForeignIncomes.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/foreign?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaForeignIncomes.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchForeign(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saForeignIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saPartnershipsIncome" should {

    "return 200 with the self tax return partnerships income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/partnerships?$requestParameters")

      given(mockSandboxSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaPartnerships.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/partnerships?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaPartnerships.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchPartnerships(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saPartnershipsIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saPensionsAndStateBenefitsIncome" should {

    "return 200 with the self tax return pensions and state benefits income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/pensions-and-state-benefits?$requestParameters")

      given(
        mockSandboxSaIncomeService.fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/pensions-and-state-benefits?$requestParametersWithoutToTaxYear")

      given(
        mockSandboxSaIncomeService.fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaPensionAndStateBenefits.transform(ifSa)))

      val result = await(
        sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(
          fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {
      given(
        mockSandboxSaIncomeService.fetchPensionAndStateBenefits(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result =
        await(sandboxSaIncomeController.saPensionsAndStateBenefitsIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saInterestsAndDividendsIncome" should {

    "return 200 with the self tax return interests and dividends income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/interests-and-dividends?$requestParameters")

      given(mockSandboxSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaInterestAndDividends.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/interests-and-dividends?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaInterestAndDividends.transform(ifSa)))

      val result = await(
        sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchInterestAndDividends(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result =
        await(sandboxSaIncomeController.saInterestsAndDividendsIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saUkPropertiesIncome" should {

    "return 200 with the UK properties income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/uk-properties?$requestParameters")

      given(mockSandboxSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaUkProperties.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2019-20"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturn": [
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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/uk-properties?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaUkProperties.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links": {
           |    "self": {
           |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2018-19"
           |    }
           |  },
           |  "selfAssessment": {
           |    "taxReturn": [
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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchUkProperties(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saUkPropertiesIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saAdditionalInformation" should {

    "return 200 with the additional information income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/additional-information?$requestParameters")

      given(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/additional-information?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaAdditionalInformationRecords.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchAdditionalInformation(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saAdditionalInformation(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saOtherIncome" should {

    "return 200 with the other income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/other?$requestParameters")

      given(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaOtherIncomeRecords.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/other?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaOtherIncomeRecords.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchOtherIncome(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saOtherIncome(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }

  "SandboxSaIncomeController.saFurtherDetails" should {

    "return 200 with the other income for the period" in new Setup {
      val fakeRequest =
        FakeRequest("GET", s"/individuals/income/further-details?$requestParameters")

      given(mockSandboxSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaFurtherDetails.transform(ifSa)))

      val result = await(sandboxSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(fakeRequest))

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
    }

    "return 200 and the self link without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear =
        FakeRequest("GET", s"/individuals/income/further-details?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(successful(SaFurtherDetails.transform(ifSa)))

      val result =
        await(sandboxSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

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
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockSandboxSaIncomeService.fetchFurtherDetails(refEq(matchId), refEq(taxYearInterval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saFurtherDetails(matchId, taxYearInterval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND
    }
  }
}
