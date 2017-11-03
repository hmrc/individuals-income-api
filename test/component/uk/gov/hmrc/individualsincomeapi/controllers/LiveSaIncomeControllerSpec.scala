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
import play.mvc.Http.Status
import play.mvc.Http.Status.{FORBIDDEN, UNAUTHORIZED}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsincomeapi.domain.{DesSAIncome, DesSAReturn, TaxYear}

import scalaj.http.Http

class LiveSaIncomeControllerSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val nino = Nino("AA100009B")
  val fromTaxYear = TaxYear("2013-14")
  val toTaxYear = TaxYear("2015-16")
  val desIncomes = Seq(
    DesSAIncome(
      taxYear = "2014",
      returnList = Seq(DesSAReturn(
        receivedDate = LocalDate.parse("2014-11-05"),
        incomeFromAllEmployments = Some(1545.55))))
  )

  feature("SA root endpoint") {

    scenario("Fetch Self Assessment annual returns") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the user")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, desIncomes)

      When("I request the self-assessments")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with the self-assessments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "employments": {"href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16"},
                 "self-employments": {"href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2013-14",
                     "submissions": [
                       {
                         "receivedDate": "2014-11-05"
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa")

      When("I request the self-assessments")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }
  }

  feature("SA employments endpoint") {
    scenario("Fetch Self Assessment employments") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-employments")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-employments")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the user")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, desIncomes)

      When("I request the employments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 200 (OK) with the employments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2013-14",
                     "employments": [
                       {
                         "employmentIncome": 1545.55
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-employments")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-employments")

      When("I request the employments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2013-14&toTaxYear=2015-16")
        .headers(requestHeaders(acceptHeaderP1)).asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }
}