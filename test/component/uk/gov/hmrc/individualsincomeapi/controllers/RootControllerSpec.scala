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

package component.uk.gov.hmrc.individualsincomeapi.controllers

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.http.Status.OK
import play.api.libs.json.Json

import scalaj.http.Http

class RootControllerSpec extends BaseSpec {

  feature("Match citizen entry point (hateoas) is open and accessible") {

    scenario("Valid request to the sandbox implementation") {
      When("I request the match citizen entry point to the API")
      val response = Http(s"$serviceUrl/sandbox?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 200 (OK)")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""{
             "_links": {
                 "paye": {
                     "href": "/individuals/income/paye?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromDate,toDate}",
                     "title": "View individual's income per employment"
                 },
                 "selfAssessment": {
                   "href": "/individuals/income/sa?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                   "title": "View individual's self-assessment income"
                 },
                 "self": {
                     "href": "/individuals/income/?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430"
                 }
             }
         }""")
    }
  }
}
