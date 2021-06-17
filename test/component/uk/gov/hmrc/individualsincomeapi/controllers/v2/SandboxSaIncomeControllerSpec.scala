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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v2

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomeData.sandboxMatchId

class SandboxSaIncomeControllerSpec extends BaseSpec {

  val matchId = sandboxMatchId
  val fromTaxYear = "2017"
  val toTaxYear = "2019"

  feature("SA root endpoint") {

    scenario("Fetch Self Assessment returns") {

      When("I request the resources")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "ukProperties": {
               |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA UK properties data"
               |    },
               |    "trusts": {
               |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA trusts data"
               |    },
               |    "selfEmployments": {
               |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA self employments data"
               |    },
               |    "partnerships": {
               |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA partnerships data"
               |    },
               |    "self": {
               |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    },
               |    "interestsAndDividends": {
               |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA interest and dividends data"
               |    },
               |    "furtherDetails": {
               |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA further details data"
               |    },
               |    "additionalInformation": {
               |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA additional information data"
               |    },
               |    "other": {
               |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA other data"
               |    },
               |    "foreign": {
               |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA foreign income data"
               |    },
               |    "summary": {
               |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA summary data"
               |    },
               |    "employments": {
               |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA employments data"
               |    },
               |    "pensionsAndStateBenefits": {
               |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA pensions and state benefits data"
               |    },
               |    "source": {
               |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA source data"
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
    }
  }

  feature("SA employments endpoint") {

    scenario("Fetch Self Assessment employments returns") {

      When("I request the resources")
      val response = Http(s"$serviceUrl/sandbox/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA self employments endpoint") {

    scenario("Fetch Self Assessment self employments returns") {

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sandbox/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA summary endpoint") {

    scenario("Fetch Self Assessment summary returns") {

      When("I request the resources")
      val response = Http(s"$serviceUrl/sandbox/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA trusts endpoint") {

    scenario("Fetch Self Assessment trusts returns") {

      When("I request the resources")
      val response = Http(s"$serviceUrl/sandbox/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA foreign endpoint") {

    scenario("Fetch Self Assessment foreign returns") {

      When("I request the resources")
      val response = Http(s"$serviceUrl/sandbox/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA partnerships endpoint") {

    scenario("Fetch Self Assessment partnerships returns") {

      When("I request the resources")
      val response = Http(s"$serviceUrl/sandbox/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA interests and dividends income") {

    scenario("Fetch Self Assessment interests and dividends returns") {

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sandbox/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA pensions and state benefits income") {

    scenario("Fetch Self Assessment pensions and benefits returns") {

      When("I request the resources")
      val response =
        Http(
          s"$serviceUrl/sandbox/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA UK properties income") {

    scenario("Fetch Self Assessment uk properties returns") {

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sandbox/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA additional information") {

    scenario("Fetch Self Assessment additional information returns") {

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sandbox/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA other income") {

    scenario("Fetch Self Assessment other income returns") {

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sandbox/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  feature("SA further details") {

    scenario("Fetch Self Assessment further details income returns") {

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sandbox/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
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
          .toString()
    }

  }

  private def headers: Map[String, String] = requestHeaders(acceptHeaderP2) + ("X-Client-ID" -> clientId)
}
