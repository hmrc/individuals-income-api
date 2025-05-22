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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v1

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.{sandboxMatchId, sandboxUtr}

class SandboxSaIncomeControllerSpec extends BaseSpec {

  val fromTaxYr = "2019-20"
  val toTaxYr = "2020-21"

  Feature("Sandbox individual income") {

    Scenario("SA root endpoint for the sandbox implementation") {

      When("I request the self-assessments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200 (OK) with a valid payload")
      val requestParameters = s"matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"

      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
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
                 "registrations": [
                   {
                     "utr": "$sandboxUtr",
                     "registrationDate": "2015-01-06"
                   }
                 ],
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "submissions": [
                       {
                         "utr": "$sandboxUtr",
                         "receivedDate": "2019-10-06"
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "submissions": [
                       {
                         "utr": "$sandboxUtr",
                         "receivedDate": "2017-06-06"
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("Employments Income endpoint for the sandbox implementation") {

      When("I request the SA employments for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/employments?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/employments?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "employments": [
                       {
                         "utr": "$sandboxUtr",
                         "employmentIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "employments": [
                       {
                         "utr": "$sandboxUtr",
                         "employmentIncome": 5000
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("Self Employments Income endpoint for the sandbox implementation") {

      When("I request the SA self employments for Sandbox")
      val response =
        Http(
          s"$serviceUrl/sandbox/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"
        )
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "selfEmployments": [
                       {
                         "utr": "$sandboxUtr",
                         "selfEmploymentProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "selfEmployments": [
                       {
                          "utr": "$sandboxUtr",
                          "selfEmploymentProfit": 10500
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("SA returns summary endpoint for the sandbox implementation") {

      When("I request the SA returns summary for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/summary?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/summary?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "summary": [
                       {
                         "utr": "$sandboxUtr",
                         "totalIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "summary": [
                       {
                          "utr": "$sandboxUtr",
                          "totalIncome": 30000
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("SA trusts endpoint for the sandbox implementation") {
      When("I request the SA trusts income for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/trusts?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/trusts?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "trusts": [
                       {
                         "utr": "$sandboxUtr",
                         "trustIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "trusts": [
                       {
                          "utr": "$sandboxUtr",
                          "trustIncome": 2143.32
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("SA foreign endpoint for the sandbox implementation") {
      When("I request the SA foreign income for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/foreign?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/foreign?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "foreign": [
                       {
                         "utr": "$sandboxUtr",
                         "foreignIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "foreign": [
                       {
                          "utr": "$sandboxUtr",
                          "foreignIncome": 1054.65
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }
  }

  Scenario("SA partnerships endpoint for the sandbox implementation") {
    When("I request the SA partnerships income for Sandbox")
    val response =
      Http(s"$serviceUrl/sandbox/sa/partnerships?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/partnerships?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "partnerships": [
                       {
                         "utr": "$sandboxUtr",
                         "partnershipProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "partnerships": [
                       {
                          "utr": "$sandboxUtr",
                          "partnershipProfit": 324.54
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }

  Scenario("SA pensions and state benefits endpoint for the sandbox implementation") {
    When("I request the SA pensions and state benefits income for Sandbox")
    val response = Http(
      s"$serviceUrl/sandbox/sa/pensions-and-state-benefits?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"
    )
      .headers(requestHeaders(acceptHeaderP1))
      .asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "pensionsAndStateBenefits": [
                       {
                         "utr": "$sandboxUtr",
                         "totalIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "pensionsAndStateBenefits": [
                       {
                          "utr": "$sandboxUtr",
                          "totalIncome": 52.79
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }

  Scenario("SA interests and dividends endpoint for the sandbox implementation") {
    When("I request the SA interests and dividends income for Sandbox")
    val response = Http(
      s"$serviceUrl/sandbox/sa/interests-and-dividends?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"
    )
      .headers(requestHeaders(acceptHeaderP1))
      .asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/interests-and-dividends?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "interestsAndDividends": [
                       {
                         "utr": "$sandboxUtr",
                         "ukInterestsIncome": 0,
                         "foreignDividendsIncome": 0,
                         "ukDividendsIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "interestsAndDividends": [
                       {
                          "utr": "$sandboxUtr",
                          "ukInterestsIncome": 12.46,
                          "foreignDividendsIncome": 25.86,
                          "ukDividendsIncome": 657.89
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }

  Scenario("SA uk-properties endpoint for the sandbox implementation") {
    When("I request the SA uk-properties income for Sandbox")
    val response =
      Http(s"$serviceUrl/sandbox/sa/uk-properties?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/uk-properties?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "ukProperties": [
                       {
                         "utr": "$sandboxUtr",
                         "totalProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "ukProperties": [
                       {
                          "utr": "$sandboxUtr",
                          "totalProfit": 1276.67
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }

  Scenario("SA additional-information endpoint for the sandbox implementation") {
    When("I request the SA additional-information income for Sandbox")
    val response = Http(
      s"$serviceUrl/sandbox/sa/additional-information?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"
    )
      .headers(requestHeaders(acceptHeaderP1))
      .asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/additional-information?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "additionalInformation": [
                       {
                         "utr": "$sandboxUtr",
                         "gainsOnLifePolicies": 0,
                         "sharesOptionsIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "additionalInformation": [
                       {
                          "utr": "$sandboxUtr",
                          "gainsOnLifePolicies": 44.54,
                          "sharesOptionsIncome": 52.34
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }

  Scenario("SA other endpoint for the sandbox implementation") {
    When("I request the SA other income for Sandbox")
    val response =
      Http(s"$serviceUrl/sandbox/sa/other?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/other?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYr&toTaxYear=$toTaxYr"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2020-21",
                     "other": [
                       {
                         "utr": "$sandboxUtr",
                         "otherIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2019-20",
                     "other": [
                       {
                          "utr": "$sandboxUtr",
                          "otherIncome": 26.70
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }
}
