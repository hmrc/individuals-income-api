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

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, DesStub, IndividualsMatchingApiStub}
import org.joda.time.LocalDate
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.individualsincomeapi.domain.{DesEmployment, DesEmployments, DesPayment}
import uk.gov.hmrc.individualsincomeapi.util.Dates
import uk.gov.hmrc.time.DateTimeUtils

import scalaj.http.Http

class IntervalValidationSpec extends BaseSpec {

  val matchId = UUID.randomUUID.toString
  val nino = "CS700100A"
  val fromDate = "2016-04-01"
  lazy val today = DateTimeUtils.now.toString(Dates.localDatePattern)
  lazy val yesterday = DateTimeUtils.now.minusDays(1).toString(Dates.localDatePattern)
  val payeIncomeScope = "read:individuals-income-paye"

  feature("Date interval query parameter validation") {

    scenario("missing fromDate parameter") {

      When("I request individual income with a missing fromDate")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&toDate=2017-03-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromDate is required")
    }

    scenario("invalid format for fromDate parameter submitted") {

      When("I request individual income with an incorrectly formatted fromDate")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=20160101&toDate=2017-03-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("fromDate: invalid date format")
    }

    scenario("invalid format for toDate parameter submitted") {

      When("I request individual income with an incorrectly formatted toDate")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=2016-01-01&toDate=20170301")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("toDate: invalid date format")
    }

    scenario("invalid interval submitted. ToDate value before fromDate") {

      When("I request individual income with ToDate value before fromDate")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=2017-01-01&toDate=2016-03-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 400 (Bad Request)")
      response.code shouldBe BAD_REQUEST

      And("The correct error message is returned")
      response.body shouldBe errorResponse("Invalid time period requested")
    }

    scenario("toDate defaults to today's date when it is not provided") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK,
        s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return employments for the NINO between fromDate and today")
      DesStub.searchEmploymentIncomeForPeriodReturns(nino, fromDate, today,
        DesEmployments(Seq(DesEmployment(Seq(DesPayment(LocalDate.parse(yesterday), 100.5))))))

      When("I request individual income for the existing matchId without a toDate")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200")
      response.code shouldBe OK

      And("The response contains the payments for the period")
      Json.parse(response.body) shouldBe Json.parse(
        s"""
            {
               "_links":
               {
                 "self":
                 {
                   "href":"/individuals/income/paye?matchId=$matchId&fromDate=$fromDate"
                 }
               },
               "paye": {
                 "income":[
                   {
                     "taxablePayment":100.5,
                     "paymentDate":"$yesterday"
                   }
                 ]
               }
            }
          """)
    }
  }
}
