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

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import play.api.http.Status._
import play.api.libs.json.Json
import scalaj.http.Http

class RootControllerSpec extends BaseSpec {

  feature("Match citizen entry point (hateoas) is open and accessible") {

    scenario("Valid request to the sandbox implementation") {
      When("I request the match citizen entry point to the API")
      val response = Http(s"$serviceUrl/sandbox?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 200")
      response.code shouldBe OK

      Json.parse(response.body) shouldBe
        Json.parse(s"""
         {
            "_links":{
              "incomeSaUkProperties":{
                "href":"individuals/income/sa/uk-properties?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA UK properties data"
              },"incomeSaTrusts":{
                "href":"individuals/income/sa/trusts?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA trusts data"
              },"incomeSaSelfEmployments":{
                "href":"individuals/income/sa/self-employments?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA self employments data"
              },"incomeSaPartnerships":{
                "href":"individuals/income/sa/partnerships?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA partnerships data"
              },"self":{
                "href":"/individuals/income/?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430"
              },
              "incomeSaInterestsAndDividends":{
                "href":"individuals/income/sa/interests-and-dividends?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA interest and dividends data"
              },
              "incomeSaFurtherDetails":{
                "href":"individuals/income/sa/further-details?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA further details data"
              },"incomeSaAdditionalInformation":{
                "href":"individuals/income/sa/additional-information?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA additional information data"
              },"incomeSaOther":{
                "href":"individuals/income/sa/other?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA other data"
              },"incomeSaForeign":{
                "href":"individuals/income/sa/foreign?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA foreign income data"
              },"incomePaye":{
                "href":"individuals/income/paye?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromDate,toDate}",
                "title":"Get an individual's PAYE income data"
              },"incomeSaSummary":{
                "href":"individuals/income/sa/summary?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA summary data"
              },"incomeSaEmployments":{
                "href":"individuals/income/sa/employments?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA employments data"
              },"incomeSa":{
                "href":"individuals/income/sa?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA income data"
              },"incomeSaPensionsAndStateBenefits":{
                "href":"individuals/income/sa/pensions-and-state-benefits?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA pensions and state benefits data"
              },"incomeSaSource":{
                "href":"/individuals/income/sa/source?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430{&fromTaxYear,toTaxYear}",
                "title":"Get an individual's SA source data"
              }
            }
         }""")
    }
  }
}
