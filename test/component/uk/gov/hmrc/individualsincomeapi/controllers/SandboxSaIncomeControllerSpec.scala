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

package component.uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.{sandboxMatchId, sandboxUtr}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear

import scalaj.http.Http

class SandboxSaIncomeControllerSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val fromTaxYear = TaxYear("2013-14")
  val toTaxYear = TaxYear("2015-16")

  feature("Sandbox individual income") {

    scenario("SA root endpoint for the sandbox implementation") {

      When("I request the self-assessments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "employments": {"href": "/individuals/income/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "selfEmployments": {"href": "/individuals/income/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "summary": {"href": "/individuals/income/sa/summary?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "registrations": [
                   {
                     "utr": "$sandboxUtr",
                     "registrationDate": "2012-01-06"
                   }
                 ],
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "submissions": [
                       {
                         "utr": "$sandboxUtr",
                         "receivedDate": "2015-10-06"
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
                     "submissions": [
                       {
                         "utr": "$sandboxUtr",
                         "receivedDate": "2014-06-06"
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    scenario("Employments Income endpoint for the sandbox implementation") {

      When("I request the SA employments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "employments": [
                       {
                         "utr": "$sandboxUtr",
                         "employmentIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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

    scenario("Self Employments Income endpoint for the sandbox implementation") {

      When("I request the SA self employments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "selfEmployments": [
                       {
                         "utr": "$sandboxUtr",
                         "selfEmploymentProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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

    scenario("SA returns summary endpoint for the sandbox implementation") {

      When("I request the SA returns summary for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa/summary?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/summary?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "summary": [
                       {
                         "utr": "$sandboxUtr",
                         "totalIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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

    scenario("SA trusts endpoint for the sandbox implementation") {
      When("I request the SA trusts income for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa/trusts?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/trusts?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "trusts": [
                       {
                         "utr": "$sandboxUtr",
                         "trustIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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

    scenario("SA foreign endpoint for the sandbox implementation") {
      When("I request the SA foreign income for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa/foreign?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with a valid payload")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/foreign?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "foreign": [
                       {
                         "utr": "$sandboxUtr",
                         "foreignIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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

  scenario("SA partnerships endpoint for the sandbox implementation") {
    When("I request the SA partnerships income for Sandbox")
    val response = Http(s"$serviceUrl/sandbox/sa/partnerships?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
      .headers(requestHeaders(acceptHeaderP1)).asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(
        s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/partnerships?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "partnerships": [
                       {
                         "utr": "$sandboxUtr",
                         "partnershipProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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

  scenario("SA interests and dividends endpoint for the sandbox implementation") {
    When("I request the SA interests and dividends income for Sandbox")
    val response = Http(s"$serviceUrl/sandbox/sa/interests-and-dividends?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
      .headers(requestHeaders(acceptHeaderP1)).asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(
        s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/interests-and-dividends?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
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
                     "taxYear": "2013-14",
                     "interestsAndDividends": [
                       {
                          "utr": "$sandboxUtr",
                          "ukInterestsIncome": 12.46,
                          "foreignDividendsIncome": 455.43,
                          "ukDividendsIncome": 657.89
                       }
                     ]
                   }
                 ]
               }
             }
           """)
  }

  scenario("SA uk-properties endpoint for the sandbox implementation") {
    When("I request the SA uk-properties income for Sandbox")
    val response = Http(s"$serviceUrl/sandbox/sa/uk-properties?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
      .headers(requestHeaders(acceptHeaderP1)).asString

    Then("The response status should be 200 (OK) with a valid payload")
    response.code shouldBe OK
    Json.parse(response.body) shouldBe
      Json.parse(
        s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/uk-properties?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2014-15",
                     "ukProperties": [
                       {
                         "utr": "$sandboxUtr",
                         "totalProfit": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
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
}

