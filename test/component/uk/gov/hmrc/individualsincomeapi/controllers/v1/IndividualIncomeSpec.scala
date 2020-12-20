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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v1

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, DesStub, IndividualsMatchingApiStub}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxMatchId
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesEmployment, DesEmployments, DesPayment}

class IndividualIncomeSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val nino = "CS700100A"
  val fromDate = "2019-04-01"
  val toDate = "2020-01-01"
  val payeIncomeScope = "read:individuals-income-paye"

  feature("Live individual income") {

    scenario("Individual has employment income") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("DES will return employments for the NINO")
      DesStub.searchEmploymentIncomeForPeriodReturns(
        nino,
        fromDate,
        toDate,
        DesEmployments(
          Seq(DesEmployment(
            employerDistrictNumber = Some("123"),
            employerSchemeReference = Some("DI45678"),
            payments = Seq(
              DesPayment(LocalDate.parse("2020-02-09"), 500.25, weekPayNumber = Some(45)),
              DesPayment(LocalDate.parse("2020-02-16"), 500.25, weekPayNumber = Some(46)),
              DesPayment(LocalDate.parse("2019-04-15"), 1000.25, monthPayNumber = Some(1)),
              DesPayment(LocalDate.parse("2019-05-15"), 1000.25, weekPayNumber = Some(2))
            )
          )))
      )

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {
                   "href": "/individuals/income/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate"
                 }
               },
               "paye": {
                 "income": [
                   {
                     "taxablePayment": 500.25,
                     "paymentDate": "2020-02-16",
                     "employerPayeReference": "123/DI45678",
                     "weekPayNumber": 46
                   },
                   {
                     "taxablePayment": 500.25,
                     "paymentDate": "2020-02-09",
                     "employerPayeReference": "123/DI45678",
                     "weekPayNumber": 45
                   },
                   {
                     "taxablePayment": 1000.25,
                     "paymentDate": "2019-05-15",
                     "employerPayeReference": "123/DI45678",
                     "weekPayNumber": 2
                   },
                   {
                     "taxablePayment": 1000.25,
                     "paymentDate": "2019-04-15",
                     "employerPayeReference": "123/DI45678",
                     "monthPayNumber": 1
                   }
                 ]
               }
             }
           """)
    }

    scenario("Individual has no employment income") {
      val toDate = "2020-02-01"

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("DES will return employments for the NINO")
      DesStub.searchEmploymentIncomeReturnsNoIncomeFor(nino, fromDate, toDate)

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {
                   "href": "/individuals/income/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate"
                 }
               },
               "paye": {
                 "income": []
               }
             }
           """)
    }

    scenario("The employment income data source is rate limited") {
      val toDate = "2020-02-02"

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, payeIncomeScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("DES is rate limited")
      DesStub.searchEmploymentIncomeReturnsRateLimitErrorFor(nino, fromDate, toDate)

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  feature("Sandbox individual income") {

    scenario("Valid request to the sandbox implementation") {

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$sandboxMatchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {
                   "href": "/individuals/income/paye?matchId=$sandboxMatchId&fromDate=$fromDate&toDate=$toDate"
                 }
               },
               "paye": {
                 "income": [
                   {
                     "taxablePayment": 1000.25,
                     "paymentDate": "2019-05-28",
                     "employerPayeReference": "123/AI45678",
                     "monthPayNumber": 2
                   },
                   {
                     "taxablePayment": 1000.25,
                     "paymentDate": "2019-04-28",
                     "employerPayeReference": "123/AI45678",
                     "monthPayNumber": 1
                   }
                 ]
               }
             }
           """)
    }
  }
}
