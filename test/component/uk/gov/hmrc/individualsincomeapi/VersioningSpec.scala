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

package component.uk.gov.hmrc.individualsincomeapi

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION}
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxMatchId

import scalaj.http.{Http, HttpResponse}

class VersioningSpec extends BaseSpec {

  implicit override lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      "auditing.enabled"                -> false,
      "auditing.traceRequests"          -> false,
      "microservice.services.auth.port" -> AuthStub.port,
      "run.mode"                        -> "It"
    )
    .build()
  val incomeScope = "read:individuals-income"

  feature("Versioning") {

    scenario("Requests with an accept header version P1.0") {
      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

      When("A request to the match citizen endpoint is made with version P1.0 accept header")
      val response = invokeWithHeaders(s"/sandbox?matchId=$sandboxMatchId", AUTHORIZATION -> authToken, acceptHeaderP1)

      Then("The response status should be 200 (Ok)")
      response.code shouldBe OK

      And("The response body should be for api version P1.0")
      Json.parse(response.body) shouldBe validResponsePayload
    }

    scenario("Requests with an accept header version P2.0") {
      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

      When("A request to the match citizen endpoint is made with version P2.0 accept header")
      val response = invokeWithHeaders(s"/sandbox?matchId=$sandboxMatchId", AUTHORIZATION -> authToken, acceptHeaderP2)

      Then("The response status should be 200")
      response.code shouldBe OK

      And("And the response body should be for api version P2.0")
      Json.parse(response.body) shouldBe validResponsePayloadP2
    }

    scenario("Requests without an accept header default to version 1.0") {
      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

      When("A request to the match citizen endpoint is made without an accept header")
      val response = invokeWithHeaders(s"/sandbox?matchId=$sandboxMatchId", AUTHORIZATION -> authToken)

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }

    scenario("Requests with an accept header with an invalid version") {
      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, incomeScope)

      When("A request to the match citizen endpoint is made with version 1.0 accept header")
      val response = invokeWithHeaders(
        s"/sandbox?matchId=$sandboxMatchId",
        AUTHORIZATION -> authToken,
        ACCEPT        -> "application/vnd.hmrc.1.0+json")

      Then("The response status should be 404 (Not Found)")
      response.code shouldBe NOT_FOUND
    }
  }

  private def validResponsePayload =
    Json.parse(s"""
         {
             "_links": {
                 "paye": {
                     "href": "/individuals/income/paye?matchId=$sandboxMatchId{&fromDate,toDate}",
                     "title": "View individual's income per employment"
                 },
                 "selfAssessment": {
                   "href": "/individuals/income/sa?matchId=$sandboxMatchId{&fromTaxYear,toTaxYear}",
                   "title": "View individual's self-assessment income"
                 },
                 "self": {
                     "href": "/individuals/income/?matchId=$sandboxMatchId"
                 }
             }
         }""")

  private def validResponsePayloadP2 =
    Json.parse(s"""{
                  |  "_links": {
                  |    "incomeSa": {
                  |      "href": "individuals/income/sa?matchId=$sandboxMatchId{&fromTaxYear,toTaxYear}",
                  |      "title": "Get an individual's SA income data"
                  |    },
                  |    "incomePaye": {
                  |      "href": "individuals/income/paye?matchId=$sandboxMatchId{&fromDate,toDate}",
                  |      "title": "Get an individual's PAYE income data"
                  |    },
                  |    "self": {
                  |      "href": "/individuals/income/?matchId=$sandboxMatchId"
                  |    }
                  |  }
                  |}""".stripMargin)

  private def invokeWithHeaders(urlPath: String, headers: (String, String)*): HttpResponse[String] =
    Http(s"$serviceUrl$urlPath").headers(headers).asString
}
