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

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxMatchId

import scalaj.http.Http

class IndividualIncomeSpec extends BaseSpec {

  feature("individual income is open and accessible") {

    scenario("Valid request to the sandbox implementation") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken)

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/paye/match/$sandboxMatchId?fromDate=2016-04-01&toDate=2017-01-01")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {
                   "href": "/individuals/income/paye/match/$sandboxMatchId?fromDate=2016-04-01&toDate=2017-01-01"
                 }
               },
               "_embedded": {
                 "income": [
                   {
                     "taxablePayment": 1000.25,
                     "paymentDate": "2016-05-28",
                     "employerPayeReference": "123/AI45678",
                     "monthPayNumber": 2
                   },
                   {
                     "taxablePayment": 1000.25,
                     "paymentDate": "2016-04-28",
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
