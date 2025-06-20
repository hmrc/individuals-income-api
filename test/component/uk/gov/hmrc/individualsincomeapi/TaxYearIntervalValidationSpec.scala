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

package component.uk.gov.hmrc.individualsincomeapi

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxMatchId

import java.time.{LocalDateTime, ZoneId}

class TaxYearIntervalValidationSpec extends BaseSpec {

  val fromYear = "2015-16"
  val today = LocalDateTime.now(ZoneId.of("Europe/London"))

  Feature("Tax Year Interval query parameter validation") {

    Scenario("missing fromTaxYear parameter") {

      When("I request individual SA income with a missing fromTaxYear")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromTaxYear is required")
    }

    Scenario("invalid format for fromTaxYear parameter submitted") {

      When("I request individual SA income with an incorrectly formatted fromTaxYear")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=20162017&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromTaxYear: invalid tax year format")
    }

    Scenario("invalid format for toTaxYear parameter submitted") {

      When("I request individual SA income with an incorrectly formatted toTaxYear")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=20170301")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("toTaxYear: invalid tax year format")
    }

    Scenario("invalid interval submitted. toTaxYear before fromTaxYear") {

      When("I request individual income with toTaxYear value before fromTaxYear")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=2016-17&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("Invalid time period requested")
    }

    Scenario("fromTaxYear earlier than maximum allowed") {

      When("I request individual income with fromTaxYear 7 years before the current tax year")
      val fromTaxYear = TaxYear.fromEndYear(today.getYear - 8)
      val response = Http(
        s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${fromTaxYear.formattedTaxYear}"
      )
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromTaxYear earlier than allowed (CY-6)")
    }

    Scenario("toTaxYear later than the current tax year") {

      When("I request individual income with a toTaxYear later than the current tax year")
      val currentEndYear = TaxYear.current().endYr
      val fromTaxYear = TaxYear.fromEndYear(today.getYear - 3).formattedTaxYear
      val toTaxYear = TaxYear.fromEndYear(currentEndYear + 1).formattedTaxYear
      val response =
        Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=$fromTaxYear&toTaxYear=$toTaxYear")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("toTaxYear is later than the current tax year")
    }

    Scenario("toTaxYear defaults to the current tax year when it is not provided") {

      When("I request individual income for the existing matchId without a toTaxYear")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=2020-21")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200")
      response.code shouldBe OK

      And("The response contains the self-assessment for the period")
      (Json.parse(response.body) \ "selfAssessment" \ "taxReturns" \\ "taxYear")
        .map(_.as[String]) shouldBe Seq("2020-21")
    }
  }
}
