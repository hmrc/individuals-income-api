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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v2

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, IndividualsMatchingApiStub}
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.Helpers._
import scalaj.http.{Http, HttpResponse}

import java.util.UUID

class LiveRootControllerSpec extends BaseSpec {

  val allIncomeScopes = List(
    "read:individuals-income-hmcts-c2",
    "read:individuals-income-hmcts-c3",
    "read:individuals-income-hmcts-c4",
    "read:individuals-income-ho-ecp",
    "read:individuals-income-ho-v2",
    "read:individuals-income-laa-c1",
    "read:individuals-income-laa-c2",
    "read:individuals-income-laa-c3",
    "read:individuals-income-laa-c4",
    "read:individuals-income-lsani-c1",
    "read:individuals-income-lsani-c3",
    "read:individuals-income-nictsejo-c4"
  )

  Feature("Root (hateoas) entry point is accessible") {

    val matchId = UUID.randomUUID().toString

    def invokeEndpoint(endpoint: String) =
      Http(endpoint).timeout(10000, 10000).headers(requestHeaders(acceptHeaderP2)).asString

    def assertResponseIs(
      httpResponse: HttpResponse[String],
      expectedResponseCode: Int,
      expectedResponseBody: String) = {
      httpResponse.code shouldBe expectedResponseCode
      parse(httpResponse.body) shouldBe parse(expectedResponseBody)
    }

    Scenario("invalid token") {
      Given("an invalid token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, allIncomeScopes)

      When("the root entry point to the API is invoked")
      val response = invokeEndpoint(s"$serviceUrl/?matchId=$matchId")

      Then("the response status should be 401 (unauthorized)")
      assertResponseIs(
        response,
        UNAUTHORIZED,
        """
          {
             "code" : "UNAUTHORIZED",
             "message" : "Bearer token is missing or not authorized"
          }
        """
      )
    }

    Scenario(s"user does not have valid scopes") {
      Given("A valid auth token but invalid scopes")
      AuthStub.willNotAuthorizePrivilegedAuthTokenNoScopes(authToken)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/?matchId=$matchId")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Insufficient Enrolments"
      )
    }

    Scenario("missing match id") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, allIncomeScopes)

      When("the root entry point to the API is invoked with a missing match id")
      val response = invokeEndpoint(serviceUrl)

      Then("the response status should be 400 (bad request)")
      assertResponseIs(response, BAD_REQUEST, """
          {
             "code" : "INVALID_REQUEST",
             "message" : "matchId is required"
          }
        """)
    }

    Scenario("malformed match id") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, allIncomeScopes)

      When("the root entry point to the API is invoked with a malformed match id")
      val response = invokeEndpoint(s"$serviceUrl/?matchId=malformed-match-id-value")

      Then("the response status should be 400 (bad request)")
      assertResponseIs(
        response,
        BAD_REQUEST,
        """
          {
             "code" : "INVALID_REQUEST",
             "message" : "matchId format is invalid"
          }
        """
      )
    }

    Scenario("invalid match id") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, allIncomeScopes)

      When("the root entry point to the API is invoked with an invalid match id")
      val response = invokeEndpoint(s"$serviceUrl/?matchId=$matchId")

      Then("the response status should be 404")
      assertResponseIs(
        response,
        NOT_FOUND,
        """
          {
             "code" : "NOT_FOUND",
             "message" : "The resource can not be found"
          }
        """
      )
    }

    Scenario("valid request to the live implementation") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, allIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, """
          {
            "matchId" : "951dcf9f-8dd1-44e0-91d5-cb772c8e8e5e",
            "nino" : "AB123456C"
          }
        """)

      When("the root entry point to the API is invoked with a valid match id")
      val response = invokeEndpoint(s"$serviceUrl/?matchId=$matchId")

      Then("the response status should be 200")
      response.code shouldBe OK

      Json.parse(response.body) shouldBe
        Json.parse(s"""{
                      |  "_links": {
                      |    "sa": {
                      |      "href": "/individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
                      |      "title": "Get an individual's Self Assessment income data"
                      |    },
                      |    "paye": {
                      |      "href": "/individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
                      |      "title": "Get an individual's PAYE income data per employment"
                      |    },
                      |    "self": {
                      |      "href": "/individuals/income/?matchId=$matchId"
                      |    }
                      |  }
                      |}""".stripMargin)
    }

  }

}
