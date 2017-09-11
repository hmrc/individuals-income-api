/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.http.Status._

import scalaj.http.Http

class IntervalValidationSpec extends BaseSpec {

  val matchId = UUID.randomUUID.toString

  feature("Date interval query parameter validation") {

    scenario("missing fromDate parameter") {

      When("I request individual income with a missing fromDate")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$matchId&toDate=2017-03-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromDate is required")
    }

    scenario("invalid format for fromDate parameter submitted") {

      When("I request individual income with an incorrectly formatted fromDate")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$matchId&fromDate=20160101&toDate=2017-03-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromDate: invalid date format")
    }

    scenario("invalid format for toDate parameter submitted") {

      When("I request individual income with an incorrectly formatted toDate")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$matchId&fromDate=2016-01-01&toDate=20170301")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("toDate: invalid date format")
    }

    scenario("invalid interval submitted. ToDate value before fromDate") {

      When("I request individual income with ToDate value before fromDate")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$matchId&fromDate=2017-01-01&toDate=2016-03-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("Invalid time period requested")
    }
  }
}
