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

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, IndividualsMatchingApiStub}
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.Helpers._
import scalaj.http.{Http, HttpResponse}

class LiveRootControllerSpec extends BaseSpec {

  val allIncomeScopes = List(
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

  feature("Root (hateoas) entry point is accessible") {

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

    scenario("invalid token") {
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

    scenario("missing match id") {
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

    scenario("malformed match id") {
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

    scenario("invalid match id") {
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

    scenario("valid request to the live implementation") {
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
        Json.parse(s"""
         {
            "_links":{
              "incomeSaUkProperties":{
                "href":"individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA UK properties data"
              },"incomeSaTrusts":{
                "href":"individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA trusts data"
              },"incomeSaSelfEmployments":{
                "href":"individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA self employments data"
              },"incomeSaPartnerships":{
                "href":"individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA partnerships data"
              },"self":{
                "href":"/individuals/income/?matchId=$matchId"
              },
              "incomeSaInterestsAndDividends":{
                "href":"individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA interest and dividends data"
              },
              "incomeSaFurtherDetails":{
                "href":"individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA further details data"
              },"incomeSaAdditionalInformation":{
                "href":"individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA additional information data"
              },"incomeSaOther":{
                "href":"individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA other data"
              },"incomeSaForeign":{
                "href":"individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA foreign income data"
              },"incomePaye":{
                "href":"individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
                "title":"Get an individual's PAYE income data"
              },"incomeSaSummary":{
                "href":"individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA summary data"
              },"incomeSaEmployments":{
                "href":"individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA employments data"
              },"incomeSa":{
                "href":"individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA income data"
              },"incomeSaPensionsAndStateBenefits":{
                "href":"individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA pensions and state benefits data"
              },"incomeSaSource":{
                "href":"/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA source data"
              }
            }
         }""")
    }

  }

}
