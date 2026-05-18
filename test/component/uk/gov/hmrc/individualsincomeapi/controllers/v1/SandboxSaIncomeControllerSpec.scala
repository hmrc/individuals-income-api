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

import java.time.LocalDate

class SandboxSaIncomeControllerSpec extends BaseSpec {

  private val fromYear = LocalDate.now.getYear - 2
  private val toYear = LocalDate.now.getYear - 1

  private def yearRange(r: Int) = (r + 1) % 100
  val fromTaxYr = s"$fromYear-${yearRange(fromYear)}"
  val toTaxYr = s"$toYear-${yearRange(toYear)}"

  val taxYearPrevious = s"$fromYear-${yearRange(fromYear)}"
  val taxYearCurrent = s"$toYear-${yearRange(toYear)}"

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
          "pensionsAndStateBenefits": {
            "href": "/individuals/income/sa/pensions-and-state-benefits?$requestParameters"
          },
          "self": {
            "href": "/individuals/income/sa?$requestParameters"
          },
          "partnerships": {
            "href": "/individuals/income/sa/partnerships?$requestParameters"
          },
          "ukProperties": {
            "href": "/individuals/income/sa/uk-properties?$requestParameters"
          },
          "selfEmployments": {
            "href": "/individuals/income/sa/self-employments?$requestParameters"
          },
          "foreign": {
            "href": "/individuals/income/sa/foreign?$requestParameters"
          },
          "interestsAndDividends": {
            "href": "/individuals/income/sa/interests-and-dividends?$requestParameters"
          },
          "employments": {
            "href": "/individuals/income/sa/employments?$requestParameters"
          },
          "additionalInformation": {
            "href": "/individuals/income/sa/additional-information?$requestParameters"
          },
          "trusts": {
            "href": "/individuals/income/sa/trusts?$requestParameters"
          },
          "other": {
            "href": "/individuals/income/sa/other?$requestParameters"
          },
          "summary": {
            "href": "/individuals/income/sa/summary?$requestParameters"
          }
        },
        "selfAssessment": {
          "registrations": [
            {
              "utr": "2432552635",
              "registrationDate": "2015-01-06"
            }
          ],
          "taxReturns": [
            {
              "taxYear": "2025-26",
              "submissions": [
                {
                  "utr": "2432552635",
                  "receivedDate": "2024-10-06"
                }
              ]
            },
            {
              "taxYear": "2024-25",
              "submissions": [
                {
                  "utr": "2432552635",
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
                     "taxYear": "$taxYearCurrent",
                     "employments": [
                       {
                         "utr": "$sandboxUtr",
                         "employmentIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "selfEmployments": [
                       {
                         "utr": "$sandboxUtr",
                         "selfEmploymentProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "summary": [
                       {
                         "utr": "$sandboxUtr",
                         "totalIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "trusts": [
                       {
                         "utr": "$sandboxUtr",
                         "trustIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "foreign": [
                       {
                         "utr": "$sandboxUtr",
                         "foreignIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "partnerships": [
                       {
                         "utr": "$sandboxUtr",
                         "partnershipProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "pensionsAndStateBenefits": [
                       {
                         "utr": "$sandboxUtr",
                         "totalIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
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
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "ukProperties": [
                       {
                         "utr": "$sandboxUtr",
                         "totalProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "additionalInformation": [
                       {
                         "utr": "$sandboxUtr",
                         "gainsOnLifePolicies": 0,
                         "sharesOptionsIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
                     "taxYear": "$taxYearCurrent",
                     "other": [
                       {
                         "utr": "$sandboxUtr",
                         "otherIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "$taxYearPrevious",
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
