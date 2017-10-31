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

package component.uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, DesStub, IndividualsMatchingApiStub}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.individualsincomeapi.domain.{DesEmployment, DesEmployments, DesPayment, TaxYear}
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxMatchId

import scalaj.http.Http

class SandboxSaIncomeControllerSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val fromTaxYear = TaxYear("2013-14")
  val toTaxYear = TaxYear("2015-16")

  feature("Sandbox individual income") {

    scenario("SA root endpoint for the sandbox implementation") {

      When("I request the self-assessments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "employments": {"href": "/individuals/income/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "self-employments": {"href": "/individuals/income/sa/self-employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "_embedded": {
                 "income": [
                   {
                     "taxYear": "2014-15",
                     "annualReturns": [
                       {
                         "receivedDate": "2015-10-06"
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
                     "annualReturns": [
                       {
                         "receivedDate": "2014-06-06"
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    scenario("Employments Income endpoint for the sandbox implementation") {

      When("I request the SA employments for Sandbox")
      val response = Http(s"$serviceUrl/sandbox/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/employments?matchId=$sandboxMatchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "_embedded": {
                 "income": [
                   {
                     "taxYear": "2014-15",
                     "employments": [
                       {
                         "employmentIncome": 0
                       }
                     ]
                   },
                   {
                     "taxYear": "2013-14",
                     "employments": [
                       {
                         "employmentIncome": 5000
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

  }
}