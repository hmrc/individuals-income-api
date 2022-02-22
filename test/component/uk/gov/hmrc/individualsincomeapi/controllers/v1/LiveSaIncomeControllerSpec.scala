/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesSAIncome, DesSAReturn, SAIncome}

class LiveSaIncomeControllerSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val nino = Nino("AA100009B")
  val fromTaxYear = TaxYear("2016-17")
  val toTaxYear = TaxYear("2018-19")
  val desIncomes = Seq(
    DesSAIncome(
      taxYear = "2017",
      returnList = Seq(
        DesSAReturn(
          caseStartDate = Some(LocalDate.parse("2014-01-15")),
          receivedDate = Some(LocalDate.parse("2017-11-05")),
          utr = SaUtr("2432552644"),
          addressLine1 = Some("address line 1"),
          addressLine2 = Some("address line 2"),
          addressLine3 = Some("address line 3"),
          addressLine4 = Some("address line 4"),
          postalCode = Some("postal code"),
          income = SAIncome(
            incomeFromAllEmployments = Some(1545.55),
            profitFromSelfEmployment = Some(2535.55),
            incomeFromSelfAssessment = Some(35500.55),
            incomeFromTrust = Some(10800.64),
            incomeFromForeign4Sources = Some(205.64),
            profitFromPartnerships = Some(145.67),
            incomeFromUkInterest = Some(34.56),
            incomeFromForeignDividends = Some(72.68),
            incomeFromInterestNDividendsFromUKCompaniesNTrusts = Some(90.35),
            incomeFromPensions = Some(62.56),
            incomeFromProperty = Some(257.46),
            incomeFromGainsOnLifePolicies = Some(52.34),
            incomeFromSharesOptions = Some(24.75),
            incomeFromOther = Some(134.56)
          )
        ))
    )
  )

  Feature("SA root endpoint") {

    Scenario("Fetch Self Assessment annual returns") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the user")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa root resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the self-assessments")
      val requestParameters = s"matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"

      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                  "self": {"href": "/individuals/income/sa?$requestParameters"},
                  "additionalInformation": {"href": "/individuals/income/sa/additional-information?$requestParameters"},
                  "employments": {"href": "/individuals/income/sa/employments?$requestParameters"},
                  "foreign": {"href": "/individuals/income/sa/foreign?$requestParameters"},
                  "interestsAndDividends": {"href": "/individuals/income/sa/interests-and-dividends?$requestParameters"},
                  "other": {"href": "/individuals/income/sa/other?$requestParameters"},
                  "partnerships": {"href": "/individuals/income/sa/partnerships?$requestParameters"},
                  "pensionsAndStateBenefits": {"href": "/individuals/income/sa/pensions-and-state-benefits?$requestParameters"},
                  "selfEmployments": {"href": "/individuals/income/sa/self-employments?$requestParameters"},
                  "summary": {"href": "/individuals/income/sa/summary?$requestParameters"},
                  "trusts": {"href": "/individuals/income/sa/trusts?$requestParameters"},
                  "ukProperties": {"href": "/individuals/income/sa/uk-properties?$requestParameters"}
               },
               "selfAssessment": {
                 "registrations": [
                   {
                     "utr": "2432552644",
                     "registrationDate": "2014-01-15"
                   }
                 ],
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "submissions": [
                       {
                         "utr": "2432552644",
                         "receivedDate": "2017-11-05"
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa")

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {
      val toTaxYear = TaxYear("2017-18")

      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES is rate limited")
      DesStub
        .searchSelfAssessmentIncomeForPeriodReturnsRateLimitErrorFor(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa root resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2017-18")
        .headers(headers)
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  Feature("SA employments endpoint") {
    Scenario("Fetch Self Assessment employments") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-employments")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-employments")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the user")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the employments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the employments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
             {
               "_links": {
                 "self": {"href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"}
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "employments": [
                       {
                         "utr":"2432552644",
                         "employmentIncome": 1545.55
                       }
                     ]
                   }
                 ]
               }
             }
           """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-employments")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-employments")

      When("I request the employments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA self employments endpoint") {
    Scenario("Fetch Self Assessment self employments") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-self-employments")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-self-employments")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the user")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the self employments")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the self employments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
           {
             "_links": {
               "self": {"href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"}
             },
             "selfAssessment": {
               "taxReturns": [
                 {
                   "taxYear": "2016-17",
                   "selfEmployments": [
                     {
                       "utr":"2432552644",
                       "selfEmploymentProfit": 2535.55
                     }
                   ]
                 }
               ]
             }
           }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-self-employments")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-self-employments")

      When("I request the self employments")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA summary endpoint") {
    Scenario("Fetch Self Assessment summary") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-summary")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-summary")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa summary")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the self employments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
           {
             "_links": {
               "self": {"href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"}
             },
             "selfAssessment": {
               "taxReturns": [
                 {
                   "taxYear": "2016-17",
                   "summary": [
                     {
                       "utr":"2432552644",
                       "totalIncome": 35500.55
                     }
                   ]
                 }
               ]
             }
           }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-summary")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-summary")

      When("I request the sa summary")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA trusts endpoint") {
    Scenario("Fetch Self Assessment trusts income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-trusts")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-trusts")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa trusts income")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the trusts income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
           {
             "_links": {
               "self": {"href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"}
             },
             "selfAssessment": {
               "taxReturns": [
                 {
                   "taxYear": "2016-17",
                   "trusts": [
                     {
                       "utr":"2432552644",
                       "trustIncome": 10800.64
                     }
                   ]
                 }
               ]
             }
           }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-trusts")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-trusts")

      When("I request the sa trusts income")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA foreign endpoint") {
    Scenario("Fetch Self Assessment foreign income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-foreign")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-foreign")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa foreign income")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the foreign income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "foreign": [
                       {
                         "utr": "2432552644",
                         "foreignIncome": 205.64
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-foreign")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-foreign")

      When("I request the sa foreign income")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA partnerships endpoint") {
    Scenario("Fetch Self Assessment partnerships income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-partnerships")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-partnerships")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa partnerships income")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the foreign income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "partnerships": [
                       {
                         "utr": "2432552644",
                         "partnershipProfit": 145.67
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-partnerships")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-partnerships")

      When("I request the sa partnerships income")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA interests and dividends income") {
    Scenario("Fetch Self Assessment interests and dividends income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-interests-and-dividends")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-interests-and-dividends")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa interests and dividends income")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 (OK) with the foreign income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "interestsAndDividends": [
                       {
                         "utr": "2432552644",
                         "ukInterestsIncome": 34.56,
                         "foreignDividendsIncome": 72.68,
                         "ukDividendsIncome": 90.35
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-interests-and-dividends")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-interests-and-dividends")

      When("I request the sa interests and dividends income")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA pensions and state benefits income") {
    Scenario("Fetch Self Assessment pensions and state benefits income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-pensions-and-state-benefits")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-pensions-and-state-benefits")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa pensions and state benefits income")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 (OK) with the foreign income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "pensionsAndStateBenefits": [
                       {
                         "utr": "2432552644",
                         "totalIncome": 62.56
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-pensions-and-state-benefits")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-pensions-and-state-benefits")

      When("I request the sa pensions and state benefits income")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA UK properties income") {
    Scenario("Fetch Self Assessment UK properties income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-uk-properties")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-uk-properties")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa UK properties income income")
      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the UK properties income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "ukProperties": [
                       {
                         "utr": "2432552644",
                         "totalProfit": 257.46
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-uk-properties")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-uk-properties")

      When("I request the sa UK properties income")
      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA additional information") {
    Scenario("Fetch Self Assessment additional information") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-additional-information")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-additional-information")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa additional information")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 (OK) with the UK properties income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "additionalInformation": [
                       {
                         "utr": "2432552644",
                         "gainsOnLifePolicies": 52.34,
                         "sharesOptionsIncome": 24.75
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-additional-information")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-additional-information")

      When("I request the sa additional information")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP1))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA other income") {
    Scenario("Fetch Self Assessment other income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-other")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-other")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa other income")
      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the other income")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(s"""
            {
              "_links": {
                 "self": {
                   "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
                 }
               },
               "selfAssessment": {
                 "taxReturns": [
                   {
                     "taxYear": "2016-17",
                     "other": [
                       {
                         "utr": "2432552644",
                         "otherIncome": 134.56
                       }
                     ]
                   }
                 ]
               }
             }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-other")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-other")

      When("I request the sa other income")
      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  Feature("SA source endpoint") {
    Scenario("Fetch Self Assessment source") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-source")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-source")

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("DES will return self-assessment data for the individual")
      DesStub.searchSelfAssessmentIncomeForPeriodReturns(nino, fromTaxYear, toTaxYear, clientId, desIncomes)

      When("I request the sa source")
      val response = Http(s"$serviceUrl/sa/source?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 (OK) with the sources")
      response.code shouldBe OK

      Json.parse(response.body) shouldBe
        Json.parse(s"""
           {
             "_links": {
               "self": {"href": "/individuals/income/sa/sources?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"}
             },
             "selfAssessment": {
               "taxReturns": [
                 {
                   "taxYear": "2016-17",
                   "sources" : [ {
                      "utr" : "2432552644",
                      "businessAddress" : {
                        "line1" : "address line 1",
                        "line2" : "address line 2",
                        "line3" : "address line 3",
                        "line4" : "address line 4",
                        "postcode" : "postal code"
                      }
                    } ]
                 }
               ]
             }
           }
         """)
    }

    Scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-source")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, "read:individuals-income-sa-source")

      When("I request the sa trusts income")
      val response = Http(s"$serviceUrl/sa/source?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP1))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  private def headers: Map[String, String] = requestHeaders(acceptHeaderP1) + ("X-Client-ID" -> clientId)
}


