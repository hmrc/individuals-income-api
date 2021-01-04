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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v1

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, IndividualsMatchingApiStub}
import play.api.libs.json.Json.parse
import play.api.test.Helpers._
import scalaj.http.{Http, HttpResponse}

class LiveRootControllerSpec extends BaseSpec {
  val incomeScope = "read:individuals-income"

  feature("Root (hateoas) entry point is accessible") {

    val matchId = UUID.randomUUID().toString

    def invokeEndpoint(endpoint: String) =
      Http(endpoint).timeout(10000, 10000).headers(requestHeaders(acceptHeaderP1)).asString

    def assertResponseIs(
      httpResponse: HttpResponse[String],
      expectedResponseCode: Int,
      expectedResponseBody: String) = {
      httpResponse.code shouldBe expectedResponseCode
      parse(httpResponse.body) shouldBe parse(expectedResponseBody)
    }

    scenario("invalid token") {
      Given("an invalid token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, incomeScope)

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

    scenario("missing match id") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

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

    scenario("malformed match id") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

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

    scenario("invalid match id") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

      When("the root entry point to the API is invoked with an invalid match id")
      val response = invokeEndpoint(s"$serviceUrl/?matchId=$matchId")

      Then("the response status should be 404 (not found)")
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

    scenario("valid request to the live implementation") {
      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, """
          {
            "matchId" : "951dcf9f-8dd1-44e0-91d5-cb772c8e8e5e",
            "nino" : "AB123456C"
          }
        """)

      When("the root entry point to the API is invoked with a valid match id")
      val response = invokeEndpoint(s"$serviceUrl/?matchId=$matchId")

      Then("the response status should be 200 (ok)")
      assertResponseIs(
        response,
        OK,
        s"""
          {
            "_links":{
              "paye":{
                "href":"/individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
                "title":"View individual's income per employment"
              },
              "selfAssessment":{
                "href":"/individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"View individual's self-assessment income"
              },
              "self":{
                "href":"/individuals/income/?matchId=$matchId"
              }
            }
          }
        """
      )
    }

  }

}
