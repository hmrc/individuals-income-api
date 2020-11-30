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
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxMatchId

class IndividualIncomeSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val nino = "CS700100A"
  val fromDate = "2019-04-01"
  val toDate = "2020-01-01"

  val payeIncomeScopes = List(
    "read:individuals-employments-nictsejo-c4",
    "read:individuals-income-hmcts-c2",
    "read:individuals-income-hmcts-c3",
    "read:individuals-income-hmcts-c4",
    "read:individuals-income-laa-c1",
    "read:individuals-income-laa-c2",
    "read:individuals-income-laa-c3",
    "read:individuals-income-laa-c4",
    "read:individuals-income-lsani-c1",
    "read:individuals-income-lsani-c3"
  )

  feature("Live individual income") {

    scenario("Individual has employment income") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("IF will return employments for the NINO")
      // TODO: Fill in

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Individual has no employment income") {
      val toDate = "2020-02-01"

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("IF will return employments for the NINO")
      // TODO: Fill in

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("The employment income data source is rate limited") {
      val toDate = "2020-02-02"

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("IF is rate limited")
      // TODO: Fill in

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }

  feature("Sandbox individual income") {

    scenario("Valid request to the sandbox implementation") {

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$sandboxMatchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }
}