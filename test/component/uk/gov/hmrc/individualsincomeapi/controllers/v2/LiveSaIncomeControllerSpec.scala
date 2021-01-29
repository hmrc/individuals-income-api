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

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, IfStub, IndividualsMatchingApiStub}
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSa
import utils.IncomeSaHelpers

class LiveSaIncomeControllerSpec extends BaseSpec with IncomeSaHelpers {

  val matchId = UUID.randomUUID().toString
  val fromTaxYear = "2017"
  val toTaxYear = "2019"

  val incomeSaSingle = IfSa(Seq(createValidSaTaxYearEntry()))

  val fields = "sa(returnList(addressLine1,addressLine2,addressLine3,addressLine4,busEndDate," +
    "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
    "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
    "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,postcode," +
    "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

  feature("SA root endpoint") {

    val nino = "AA100009B"

    val rootScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-hmcts-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-laa-c4",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "ukProperties": {
               |      "href": "individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA UK properties data"
               |    },
               |    "trusts": {
               |      "href": "individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA trusts data"
               |    },
               |    "employments": {
               |      "href": "individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA self employments data"
               |    },
               |    "partnerships": {
               |      "href": "individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA partnerships data"
               |    },
               |    "self": {
               |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    },
               |    "interestsAndDividends": {
               |      "href": "individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA interest and dividends data"
               |    },
               |    "furtherDetails": {
               |      "href": "individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA further details data"
               |    },
               |    "additionalInformation": {
               |      "href": "individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA additional information data"
               |    },
               |    "other": {
               |      "href": "individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA other data"
               |    },
               |    "foreign": {
               |      "href": "individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA foreign income data"
               |    },
               |    "summary": {
               |      "href": "individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA summary data"
               |    },
               |    "employments": {
               |      "href": "individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA employments data"
               |    },
               |    "pensionsAndStateBenefits": {
               |      "href": "individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
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
          .toString()
    }

    scenario("Fetch Self Assessment returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "ukProperties": {
               |      "href": "individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA UK properties data"
               |    },
               |    "trusts": {
               |      "href": "individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA trusts data"
               |    },
               |    "selfEmployments": {
               |      "href": "individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA self employments data"
               |    },
               |    "partnerships": {
               |      "href": "individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA partnerships data"
               |    },
               |    "self": {
               |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    },
               |    "interestsAndDividends": {
               |      "href": "individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA interest and dividends data"
               |    },
               |    "furtherDetails": {
               |      "href": "individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA further details data"
               |    },
               |    "additionalInformation": {
               |      "href": "individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA additional information data"
               |    },
               |    "other": {
               |      "href": "individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA other data"
               |    },
               |    "foreign": {
               |      "href": "individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA foreign income data"
               |    },
               |    "summary": {
               |      "href": "individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA summary data"
               |    },
               |    "employments": {
               |      "href": "individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA employments data"
               |    },
               |    "pensionsAndStateBenefits": {
               |      "href": "individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA pensions and state benefits data"
               |    },
               |    "source": {
               |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA source data"
               |    }
               |  },
               |  "selfAssessment": {
               |    "registrations": [],
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA employments endpoint") {

    val nino = "AA100009C"

    val employmentScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/employments?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment employments returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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
               |            "employmentIncome": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Fetch Self Assessment employments returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA self employments endpoint") {

    val nino = "AA100009C"

    val fields =
      "sa(returnList(addressLine1,addressLine2,addressLine3,addressLine4,busEndDate,busStartDate," +
        "businessDescription,caseStartDate,deducts(totalBusExpenses),income(allEmployments," +
        "foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment,selfEmployment,shares," +
        "trusts,ukDivsAndInterest,ukInterest,ukProperty),postcode,receivedDate,telephoneNumber," +
        "totalNIC,totalTaxPaid),taxYear)"

    val selfAssessmentScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/self-employments?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment self employments returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment self employments returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA summary endpoint") {

    val nino = "AA100009D"

    val summaryScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/summary?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment summary returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment summary returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA trusts endpoint") {

    val nino = "AA100001D"

    val trustsScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/trusts?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment trusts returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment trusts returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA foreign endpoint") {

    val nino = "AA100002D"

    val fields =
      "sa(returnList(addressLine1,addressLine2,addressLine3,addressLine4,busEndDate,busStartDate," +
        "businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
        "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
        "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,postcode," +
        "receivedDate,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

    val foreignScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/foreign?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment foreign returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment foreign returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }

  }

  feature("SA partnerships endpoint") {

    val nino = "AA100003D"

    val partnershipsScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/partnerships?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment partnerships returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment partnerships returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA interests and dividends income") {

    val nino = "AA100003D"

    val fields =
      "sa(returnList(addressLine1,addressLine2,addressLine3,addressLine4,busEndDate,busStartDate," +
        "businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
        "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
        "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,postcode," +
        "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

    val interestsAndDividendsScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the API is invoked")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/interests-and-dividends?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the API is invoked with a malformed match id")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment interests and dividends returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment interests and dividends returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
          .headers(headers)
          .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA pensions and state benefits income") {

    val nino = "AA100004D"

    val pensionsAndStateBenefitsScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the API is invoked")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/pensions-and-state-benefits?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the API is invoked with a malformed match id")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment pensions and benefits returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment pensions and benefits returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
          .headers(headers)
          .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA UK properties income") {

    val nino = "AA100005D"

    val ukPropertiesScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the API is invoked")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/uk-properties?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the API is invoked with a malformed match id")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment uk properties returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment uk properties returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
          .headers(headers)
          .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA additional information") {

    val nino = "AA100006D"

    val additionalInformationScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the API is invoked")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/additional-information?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the API is invoked with a malformed match id")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment additional information returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment additional information returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
          .headers(headers)
          .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA other income") {

    val nino = "AA100007D"

    val otherIncomeScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the API is invoked")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/other?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the API is invoked with a malformed match id")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment other income returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment other income returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
          .headers(headers)
          .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("SA further details") {

    val nino = "AA100008D"

    val fields = "sa(returnList(addressLine1,addressLine2,addressLine3,addressLine4,busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,postcode,receivedDate," +
      "totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

    val furtherDetailsScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the API is invoked")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the API is invoked with a missing match id")
      val response = Http(s"$serviceUrl/sa/further-details?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the API is invoked with a malformed match id")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }

    scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/sa/further-details?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }

    scenario("Fetch Self Assessment further details income returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
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

    scenario("Fetch Self Assessment further details income returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
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
               |      "href": "/individuals/income/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    scenario("Invalid token") {

      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
          .headers(headers)
          .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  private def headers: Map[String, String] = requestHeaders(acceptHeaderP2) + ("X-Client-ID" -> clientId)
}
