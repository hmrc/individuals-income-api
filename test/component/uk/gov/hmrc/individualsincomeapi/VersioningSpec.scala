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

package component.uk.gov.hmrc.individualsincomeapi

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION}
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxMatchId

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
    Json.parse(s"""
         {
            "_links":{
              "incomeSaUkProperties":{
                "href":"individuals/income/sa/uk-properties?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA UK properties data"
              },"incomeSaTrusts":{
                "href":"individuals/income/sa/trusts?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA trusts data"
              },"incomeSaSelfEmployments":{
                "href":"individuals/income/sa/self-employments?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA self employments data"
              },"incomeSaPartnerships":{
                "href":"individuals/income/sa/partnerships?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA partnerships data"
              },"self":{
                "href":"/individuals/income/?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430"
              },
              "incomeSaInterestsAndDividends":{
                "href":"individuals/income/sa/interests-and-dividends?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA interest and dividends data"
              },
              "incomeSaFurtherDetails":{
                "href":"individuals/income/sa/further-details?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA further details data"
              },"incomeSaAdditionalInformation":{
                "href":"individuals/income/sa/additional-information?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA additional information data"
              },"incomeSaOther":{
                "href":"individuals/income/sa/other?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA other data"
              },"incomeSaForeign":{
                "href":"individuals/income/sa/foreign?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA foreign income data"
              },"incomePaye":{
                "href":"individuals/income/paye?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startDate,endDate}",
                "title":"Get an individual's PAYE income data"
              },"incomeSaSummary":{
                "href":"individuals/income/sa/summary?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA summary data"
              },"incomeSaEmployments":{
                "href":"individuals/income/sa/employments?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA employments data"
              },"incomeSa":{
                "href":"individuals/income/sa?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA income data"
              },"incomeSaPensionsAndStateBenefits":{
                "href":"individuals/income/sa/pensions-and-state-benefits?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA pensions and state benefits data"
              },"incomeSaSource":{
                "href":"/individuals/income/sa/source?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&startYear,endYear}",
                "title":"Get an individual's SA source data"
              }
            }
         }""")

  private def invokeWithHeaders(urlPath: String, headers: (String, String)*): HttpResponse[String] =
    Http(s"$serviceUrl$urlPath").headers(headers).asString
}
