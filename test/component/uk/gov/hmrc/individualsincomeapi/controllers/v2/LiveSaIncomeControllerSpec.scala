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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v2

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, DesStub, IfStub, IndividualsMatchingApiStub}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSa
import utils.IncomeSaHelpers

class LiveSaIncomeControllerSpec extends BaseSpec with IncomeSaHelpers {

  val matchId = UUID.randomUUID().toString
  val nino = "AA100009B"
  val fromTaxYear = "2017"
  val toTaxYear = "2019"

  val fields = "sa(returnList(addressLine1,addressLine2,addressLine3,addressLine4,busEndDate," +
    "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
    "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
    "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,postcode," +
    "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

  val incomeSaSingle = IfSa(Seq(createValidSaTaxYearEntry()))

  feature("SA root endpoint") {

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
        "message" -> "fromTaxYear earlier than 31st March 2013"
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
        "message" -> "fromTaxYear: invalid date format"
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
        "message" -> "toTaxYear: invalid date format"
      )

    }

    scenario("Fetch Self Assessment annual returns") {
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

      When("I request the sa root resources")
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
               |    "incomeSaUkProperties": {
               |      "href": "individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA UK properties data"
               |    },
               |    "incomeSaTrusts": {
               |      "href": "individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA trusts data"
               |    },
               |    "incomeSaSelfEmployments": {
               |      "href": "individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA self employments data"
               |    },
               |    "incomeSaPartnerships": {
               |      "href": "individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA partnerships data"
               |    },
               |    "self": {
               |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    },
               |    "incomeSaInterestsAndDividends": {
               |      "href": "individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA interest and dividends data"
               |    },
               |    "incomeSaFurtherDetails": {
               |      "href": "individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA further details data"
               |    },
               |    "incomeSaAdditionalInformation": {
               |      "href": "individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA additional information data"
               |    },
               |    "incomeSaOther": {
               |      "href": "individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA other data"
               |    },
               |    "incomeSaForeign": {
               |      "href": "individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA foreign income data"
               |    },
               |    "incomePaye": {
               |      "href": "individuals/income/paye?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's PAYE income data"
               |    },
               |    "incomeSaSummary": {
               |      "href": "individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA summary data"
               |    },
               |    "incomeSaEmployments": {
               |      "href": "individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA employments data"
               |    },
               |    "incomeSaPensionsAndStateBenefits": {
               |      "href": "individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA pensions and state benefits data"
               |    },
               |    "incomeSaSource": {
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

    scenario("Fetch Self Assessment annual returns no root data") {
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

      When("I request the sa root resources")
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
               |    "incomeSaUkProperties": {
               |      "href": "individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA UK properties data"
               |    },
               |    "incomeSaTrusts": {
               |      "href": "individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA trusts data"
               |    },
               |    "incomeSaSelfEmployments": {
               |      "href": "individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA self employments data"
               |    },
               |    "incomeSaPartnerships": {
               |      "href": "individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA partnerships data"
               |    },
               |    "self": {
               |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    },
               |    "incomeSaInterestsAndDividends": {
               |      "href": "individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA interest and dividends data"
               |    },
               |    "incomeSaFurtherDetails": {
               |      "href": "individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA further details data"
               |    },
               |    "incomeSaAdditionalInformation": {
               |      "href": "individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA additional information data"
               |    },
               |    "incomeSaOther": {
               |      "href": "individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA other data"
               |    },
               |    "incomeSaForeign": {
               |      "href": "individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA foreign income data"
               |    },
               |    "incomePaye": {
               |      "href": "individuals/income/paye?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's PAYE income data"
               |    },
               |    "incomeSaSummary": {
               |      "href": "individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA summary data"
               |    },
               |    "incomeSaEmployments": {
               |      "href": "individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA employments data"
               |    },
               |    "incomeSaPensionsAndStateBenefits": {
               |      "href": "individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
               |      "title": "Get an individual's SA pensions and state benefits data"
               |    },
               |    "incomeSaSource": {
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

      When("I request the sa root resources")
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

//  feature("SA employments endpoint") {
//
//    val employmentScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4",
//    )
//
//    scenario("Fetch Self Assessment employments") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-employments")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the user")
//      // TODO: Fill in
//
//      When("I request the employments")
//      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the employments")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-employments")
//      AuthStub
//        .willNotAuthorizePrivilegedAuthToken(authToken, employmentScopes)
//
//      When("I request the employments")
//      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA self employments endpoint") {
//
//    val selfAssessmentScopes = List(
//      "read:individuals-income-hmcts-c2",
//      "read:individuals-income-hmcts-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4"
//    )
//
//    scenario("Fetch Self Assessment self employments") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-self-employments")
//      AuthStub
//        .willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the user")
//      // TODO: Fill in
//
//      When("I request the self employments")
//      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the self employments")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-self-employments")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)
//
//      When("I request the self employments")
//      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA summary endpoint") {
//
//    val summaryScopes = List(
//      "read:individuals-income-hmcts-c2",
//      "read:individuals-income-hmcts-c3",
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4"
//    )
//
//    scenario("Fetch Self Assessment summary") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-summary")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa summary")
//      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the self employments")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-summary")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, summaryScopes)
//
//      When("I request the sa summary")
//      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA trusts endpoint") {
//
//    val trustsScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4",
//    )
//
//    scenario("Fetch Self Assessment trusts income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-trusts")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa trusts income")
//      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the trusts income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-trusts")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, trustsScopes)
//
//      When("I request the sa trusts income")
//      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA foreign endpoint") {
//
//    val foreignScopes = List(
//      "read:individuals-income-hmcts-c2",
//      "read:individuals-income-hmcts-c3",
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3"
//    )
//
//    scenario("Fetch Self Assessment foreign income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-foreign")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa foreign income")
//      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the foreign income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-foreign")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, foreignScopes)
//
//      When("I request the sa foreign income")
//      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA partnerships endpoint") {
//
//    val partnershipsScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4",
//    )
//
//    scenario("Fetch Self Assessment partnerships income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-partnerships")
//      AuthStub
//        .willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa partnerships income")
//      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the foreign income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-partnerships")
//      AuthStub
//        .willNotAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)
//
//      When("I request the sa partnerships income")
//      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA interests and dividends income") {
//
//    val interestsAndDividendsScopes = List(
//      "read:individuals-income-hmcts-c2",
//      "read:individuals-income-hmcts-c3",
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4",
//    )
//
//    scenario("Fetch Self Assessment interests and dividends income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-interests-and-dividends")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa interests and dividends income")
//      val response =
//        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//          .headers(headers)
//          .asString
//
//      Then("The response status should be 500 with the foreign income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-interests-and-dividends")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)
//
//      When("I request the sa interests and dividends income")
//      val response =
//        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//          .headers(requestHeaders(acceptHeaderP2))
//          .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA pensions and state benefits income") {
//
//    val pensionsAndStateBenefitsScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4"
//    )
//
//    scenario("Fetch Self Assessment pensions and state benefits income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-pensions-and-state-benefits")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa pensions and state benefits income")
//      val response =
//        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//          .headers(headers)
//          .asString
//
//      Then("The response status should be 500 with the foreign income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-pensions-and-state-benefits")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)
//
//      When("I request the sa pensions and state benefits income")
//      val response =
//        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//          .headers(requestHeaders(acceptHeaderP2))
//          .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA UK properties income") {
//
//    val ukPropertiesScopes = List(
//      "read:individuals-income-hmcts-c2",
//      "read:individuals-income-hmcts-c3",
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4"
//    )
//
//    scenario("Fetch Self Assessment UK properties income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-uk-properties")
//      AuthStub
//        .willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa UK properties income income")
//      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the UK properties income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-uk-properties")
//      AuthStub
//        .willNotAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)
//
//      When("I request the sa UK properties income")
//      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA additional information") {
//
//    val additionalInformationScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4"
//    )
//
//    scenario("Fetch Self Assessment additional information") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-additional-information")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa additional information")
//      val response =
//        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//          .headers(headers)
//          .asString
//
//      Then("The response status should be 500 with the UK properties income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-additional-information")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)
//
//      When("I request the sa additional information")
//      val response =
//        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//          .headers(requestHeaders(acceptHeaderP2))
//          .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA other income") {
//
//    val otherIncomeScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3",
//      "read:individuals-income-nictsejo-c4"
//    )
//
//    scenario("Fetch Self Assessment other income") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-other")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa other income")
//      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the other income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-other")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)
//
//      When("I request the sa other income")
//      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }
//
//  feature("SA further details") {
//
//    val furtherDetailsScopes = List(
//      "read:individuals-income-laa-c1",
//      "read:individuals-income-laa-c2",
//      "read:individuals-income-laa-c3",
//      "read:individuals-income-lsani-c1",
//      "read:individuals-income-lsani-c3"
//    )
//
//    scenario("Fetch Self Assessment further details") {
//      Given("A privileged Auth bearer token with scope read:individuals-income-sa-further-details")
//      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)
//
//      And("a valid record in the matching API")
//      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")
//
//      And("IF will return self-assessment data for the individual")
//      // TODO: Fill in
//
//      When("I request the sa further details")
//      val response = Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(headers)
//        .asString
//
//      Then("The response status should be 500 with the other income")
//      response.code shouldBe INTERNAL_SERVER_ERROR
//      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
//    }
//
//    scenario("Invalid token") {
//      Given("A token WITHOUT the scope read:individuals-income-sa-further-details")
//      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)
//
//      When("I request the sa further details")
//      val response = Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
//        .headers(requestHeaders(acceptHeaderP2))
//        .asString
//
//      Then("The response status should be 401 (Unauthorized)")
//      response.code shouldBe UNAUTHORIZED
//      Json.parse(response.body) shouldBe Json
//        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
//    }
//  }

  private def headers: Map[String, String] = requestHeaders(acceptHeaderP2) + ("X-Client-ID" -> clientId)
}
