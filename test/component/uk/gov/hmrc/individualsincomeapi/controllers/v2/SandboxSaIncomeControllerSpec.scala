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

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.{sandboxMatchId, sandboxUtr}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear

class SandboxSaIncomeControllerSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val fromTaxYear = TaxYear("2013-14")
  val toTaxYear = TaxYear("2015-16")

  feature("Sandbox individual income") {

    scenario("SA root endpoint for the sandbox implementation") {

      When("I request the self-assessments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500 with a valid payload")
      val requestParameters = s"matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19"

      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Employments Income endpoint for the sandbox implementation") {

      When("I request the SA employments for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 500 with a valid payload")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Self Employments Income endpoint for the sandbox implementation") {

      When("I request the SA self employments for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 500 with a valid payload")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("SA returns summary endpoint for the sandbox implementation") {

      When("I request the SA returns summary for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/summary?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 500 with a valid payload")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("SA trusts endpoint for the sandbox implementation") {
      When("I request the SA trusts income for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/trusts?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 500 with a valid payload")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("SA foreign endpoint for the sandbox implementation") {
      When("I request the SA foreign income for Sandbox")
      val response =
        Http(s"$serviceUrl/sandbox/sa/foreign?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 500 with a valid payload")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }

  scenario("SA partnerships endpoint for the sandbox implementation") {
    When("I request the SA partnerships income for Sandbox")
    val response =
      Http(s"$serviceUrl/sandbox/sa/partnerships?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

    Then("The response status should be 500 with a valid payload")
    response.code shouldBe INTERNAL_SERVER_ERROR
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }

  scenario("SA pensions and state benefits endpoint for the sandbox implementation") {
    When("I request the SA pensions and state benefits income for Sandbox")
    val response = Http(
      s"$serviceUrl/sandbox/sa/pensions-and-state-benefits?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
      .headers(requestHeaders(acceptHeaderP2))
      .asString

    Then("The response status should be 500 with a valid payload")
    response.code shouldBe INTERNAL_SERVER_ERROR
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }

  scenario("SA interests and dividends endpoint for the sandbox implementation") {
    When("I request the SA interests and dividends income for Sandbox")
    val response = Http(
      s"$serviceUrl/sandbox/sa/interests-and-dividends?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
      .headers(requestHeaders(acceptHeaderP2))
      .asString

    Then("The response status should be 500 with a valid payload")
    response.code shouldBe INTERNAL_SERVER_ERROR
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }

  scenario("SA uk-properties endpoint for the sandbox implementation") {
    When("I request the SA uk-properties income for Sandbox")
    val response =
      Http(s"$serviceUrl/sandbox/sa/uk-properties?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

    Then("The response status should be 500 with a valid payload")
    response.code shouldBe INTERNAL_SERVER_ERROR
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }

  scenario("SA additional-information endpoint for the sandbox implementation") {
    When("I request the SA additional-information income for Sandbox")
    val response = Http(
      s"$serviceUrl/sandbox/sa/additional-information?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
      .headers(requestHeaders(acceptHeaderP2))
      .asString

    Then("The response status should be 500 with a valid payload")
    response.code shouldBe INTERNAL_SERVER_ERROR
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }

  scenario("SA other endpoint for the sandbox implementation") {
    When("I request the SA other income for Sandbox")
    val response = Http(s"$serviceUrl/sandbox/sa/other?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2018-19")
      .headers(requestHeaders(acceptHeaderP2))
      .asString

    Then("The response status should be 500 with a valid payload")
    response.code shouldBe INTERNAL_SERVER_ERROR
    response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
  }
}
