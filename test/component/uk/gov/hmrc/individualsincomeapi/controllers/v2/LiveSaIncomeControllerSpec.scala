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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v2

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, IfStub, IndividualsMatchingApiStub}
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSa
import utils.IncomeSaHelpers

class LiveSaIncomeControllerSpec extends BaseSpec with IncomeSaHelpers {

  val matchId = UUID.randomUUID().toString
  val fromTaxYear = "2017"
  val toTaxYear = "2019"

  val nino = "CS700100A"

  val incomeSaSingle = IfSa(Seq(createValidSaTaxYearEntry()))

  val invalidIncomeSaSingle = IfSa(Seq(createValidSaTaxYearEntry().copy(taxYear = Some(""))))

  val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
    "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
    "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
    "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
    "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

  Feature("SA root endpoint") {

    val nino = "AA100009B"

    val rootScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-hmcts-c4",
      "read:individuals-income-ho-ecp",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-laa-c4",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
      "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

    testAuthorisation("sa", rootScopes)
    testMatchId("sa", rootScopes)
    testTaxYears("sa", rootScopes)
    testErrorHandling("sa", nino, fields, rootScopes)

    Scenario("Fetch Self Assessment returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""{
             |  "_links": {
             |    "ukProperties": {
             |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA UK properties data"
             |    },
             |    "trusts": {
             |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA trusts data"
             |    },
             |    "selfEmployments": {
             |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA self employments data"
             |    },
             |    "partnerships": {
             |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA partnerships data"
             |    },
             |    "self": {
             |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
             |    },
             |    "interestsAndDividends": {
             |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA interest and dividends data"
             |    },
             |    "furtherDetails": {
             |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA further details data"
             |    },
             |    "additionalInformation": {
             |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA additional information data"
             |    },
             |    "other": {
             |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA other data"
             |    },
             |    "foreign": {
             |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA foreign income data"
             |    },
             |    "summary": {
             |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA summary data"
             |    },
             |    "employments": {
             |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA employments data"
             |    },
             |    "pensionsAndStateBenefits": {
             |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA pensions and state benefits data"
             |    },
             |    "source": {
             |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA source data"
             |    }
             |  },
             |  "selfAssessment": {
             |    "registrations": [
             |      {
             |        "registrationDate": "2020-01-01",
             |        "utr": "1234567890"
             |      }
             |    ],
             |    "taxReturns": [
             |      {
             |        "taxYear": "2019-20",
             |        "submissions": [
             |          {
             |            "receivedDate": "2020-01-01",
             |            "utr": "1234567890"
             |          }
             |        ]
             |      }
             |    ]
             |  }
             |}""".stripMargin
        )
    }

    Scenario("Fetch Self Assessment returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      Json.parse(response.body) shouldBe
        Json.parse(
          s"""{
             |  "_links": {
             |    "ukProperties": {
             |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA UK properties data"
             |    },
             |    "trusts": {
             |      "href": "/individuals/income/sa/trusts?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA trusts data"
             |    },
             |    "selfEmployments": {
             |      "href": "/individuals/income/sa/self-employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA self employments data"
             |    },
             |    "partnerships": {
             |      "href": "/individuals/income/sa/partnerships?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA partnerships data"
             |    },
             |    "self": {
             |      "href": "/individuals/income/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
             |    },
             |    "interestsAndDividends": {
             |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA interest and dividends data"
             |    },
             |    "furtherDetails": {
             |      "href": "/individuals/income/sa/further-details?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA further details data"
             |    },
             |    "additionalInformation": {
             |      "href": "/individuals/income/sa/additional-information?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA additional information data"
             |    },
             |    "other": {
             |      "href": "/individuals/income/sa/other?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA other data"
             |    },
             |    "foreign": {
             |      "href": "/individuals/income/sa/foreign?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA foreign income data"
             |    },
             |    "summary": {
             |      "href": "/individuals/income/sa/summary?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA summary data"
             |    },
             |    "employments": {
             |      "href": "/individuals/income/sa/employments?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA employments data"
             |    },
             |    "pensionsAndStateBenefits": {
             |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA pensions and state benefits data"
             |    },
             |    "source": {
             |      "href": "/individuals/income/sa/source?matchId=$matchId{&fromTaxYear,toTaxYear}",
             |      "title": "Get an individual's SA source data"
             |    }
             |  },
             |  "selfAssessment": {
             |    "registrations": [],
             |    "taxReturns": []
             |  }
             |}""".stripMargin
        )
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

    val nino = "AA100009C"

    val employmentScopes = List(
      "read:individuals-income-ho-ecp",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
      "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

    testAuthorisation("sa/employments", employmentScopes)
    testMatchId("sa/employments", employmentScopes)
    testTaxYears("sa/employments", employmentScopes)
    testErrorHandling("sa/employments", nino, fields, employmentScopes)

    Scenario("Fetch Self Assessment employments returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "employments": [
               |          {
               |            "employmentIncome": 100,
               |            "utr": "1234567890"
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment employments returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA self employments endpoint") {

    val nino = "AA100009C"

    val fields =
      "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate,busStartDate," +
        "businessDescription,caseStartDate,deducts(totalBusExpenses),income(allEmployments," +
        "foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment,selfEmployment,shares," +
        "trusts,ukDivsAndInterest,ukInterest,ukProperty),receivedDate,telephoneNumber," +
        "totalNIC,totalTaxPaid,utr),taxYear)"

    val selfAssessmentScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-ho-ecp",
      "read:individuals-income-ho-v2",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    testAuthorisation("sa/self-employments", selfAssessmentScopes)
    testMatchId("sa/self-employments", selfAssessmentScopes)
    testTaxYears("sa/self-employments", selfAssessmentScopes)
    testErrorHandling("sa/self-employments", nino, fields, selfAssessmentScopes)

    Scenario("Fetch Self Assessment self employments returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "selfEmployments": [
               |          {
               |            "selfEmploymentProfit": 100,
               |            "utr": "1234567890"
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment self employments returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA summary endpoint") {

    val nino = "AA100009D"

    val summaryScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-ho-ecp",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
      "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

    testAuthorisation("sa/summary", summaryScopes)
    testMatchId("sa/summary", summaryScopes)
    testTaxYears("sa/summary", summaryScopes)
    testErrorHandling("sa/summary", nino, fields, summaryScopes)

    Scenario("Fetch Self Assessment summary returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "summary": [
               |          {
               |            "totalIncome": 100,
               |            "utr": "1234567890"
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment summary returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA trusts endpoint") {

    val nino = "AA100001D"

    val trustsScopes = List(
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    testAuthorisation("sa/trusts", trustsScopes)
    testMatchId("sa/trusts", trustsScopes)
    testTaxYears("sa/trusts", trustsScopes)
    testErrorHandling("sa/trusts", nino, fields, trustsScopes)

    Scenario("Fetch Self Assessment trusts returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "trusts": [
               |          {
               |            "trustIncome": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment trusts returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA foreign endpoint") {

    val nino = "AA100002D"

    val foreignScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
      "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

    testAuthorisation("sa/foreign", foreignScopes)
    testMatchId("sa/foreign", foreignScopes)
    testTaxYears("sa/foreign", foreignScopes)
    testErrorHandling("sa/foreign", nino, fields, foreignScopes)

    Scenario("Fetch Self Assessment foreign returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "foreign": [
               |          {
               |            "foreignIncome": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment foreign returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA partnerships endpoint") {

    val nino = "AA100003D"

    val partnershipsScopes = List(
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
      "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

    testAuthorisation("sa/partnerships", partnershipsScopes)
    testMatchId("sa/partnerships", partnershipsScopes)
    testTaxYears("sa/partnerships", partnershipsScopes)
    testErrorHandling("sa/partnerships", nino, fields, partnershipsScopes)

    Scenario("Fetch Self Assessment partnerships returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "partnerships": [
               |          {
               |            "partnershipProfit": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment partnerships returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
        .headers(headers)
        .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("I request the self assessments")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA interests and dividends income") {

    val nino = "AA100003D"

    val interestsAndDividendsScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4",
    )

    testAuthorisation("sa/interests-and-dividends", interestsAndDividendsScopes)
    testMatchId("sa/interests-and-dividends", interestsAndDividendsScopes)
    testTaxYears("sa/interests-and-dividends", interestsAndDividendsScopes)
    testErrorHandling("sa/interests-and-dividends", nino, fields, interestsAndDividendsScopes)

    Scenario("Fetch Self Assessment interests and dividends returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "interestsAndDividends": [
               |          {
               |            "ukInterestsIncome": 100,
               |            "foreignDividendsIncome": 100,
               |            "ukDividendsIncome": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment interests and dividends returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA pensions and state benefits income") {

    val nino = "AA100004D"

    val pensionsAndStateBenefitsScopes = List(
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    testAuthorisation("sa/pensions-and-state-benefits", pensionsAndStateBenefitsScopes)
    testMatchId("sa/pensions-and-state-benefits", pensionsAndStateBenefitsScopes)
    testTaxYears("sa/pensions-and-state-benefits", pensionsAndStateBenefitsScopes)
    testErrorHandling("sa/pensions-and-state-benefits", nino, fields, pensionsAndStateBenefitsScopes)

    Scenario("Fetch Self Assessment pensions and benefits returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "pensionsAndStateBenefits": [
               |          {
               |            "totalIncome": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment pensions and benefits returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("An invalid token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with valid scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA UK properties income") {

    val nino = "AA100005D"

    val ukPropertiesScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    testAuthorisation("sa/uk-properties", ukPropertiesScopes)
    testMatchId("sa/uk-properties", ukPropertiesScopes)
    testTaxYears("sa/uk-properties", ukPropertiesScopes)
    testErrorHandling("sa/uk-properties", nino, fields, ukPropertiesScopes)

    Scenario("Fetch Self Assessment uk properties returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "ukProperties": [
               |          {
               |            "totalProfit": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment uk properties returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA additional information") {

    val nino = "AA100006D"

    val additionalInformationScopes = List(
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    testAuthorisation("sa/additional-information", additionalInformationScopes)
    testMatchId("sa/additional-information", additionalInformationScopes)
    testTaxYears("sa/additional-information", additionalInformationScopes)
    testErrorHandling("sa/additional-information", nino, fields, additionalInformationScopes)

    Scenario("Fetch Self Assessment additional information returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "additionalInformation": [
               |          {
               |            "gainsOnLifePolicies": 100,
               |            "sharesOptionsIncome": 100
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment additional information returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA other income") {

    val nino = "AA100007D"

    val otherIncomeScopes = List(
      "read:individuals-income-ho-ecp",
      "read:individuals-income-ho-v2",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3",
      "read:individuals-income-nictsejo-c4"
    )

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome," +
      "receivedDate,telephoneNumber,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover,utr),taxYear)"

    testAuthorisation("sa/other", otherIncomeScopes)
    testMatchId("sa/other", otherIncomeScopes)
    testTaxYears("sa/other", otherIncomeScopes)
    testErrorHandling("sa/other", nino, fields, otherIncomeScopes)

    Scenario("Fetch Self Assessment other income returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "other": [
               |          {
               |            "otherIncome": 100,
               |            "utr": "1234567890"
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment other income returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  Feature("SA further details") {

    val nino = "AA100008D"

    val fields = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate," +
      "busStartDate,businessDescription,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,receivedDate," +
      "totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

    val furtherDetailsScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    testAuthorisation("sa/further-details", furtherDetailsScopes)
    testMatchId("sa/further-details", furtherDetailsScopes)
    testTaxYears("sa/further-details", furtherDetailsScopes)
    testErrorHandling("sa/further-details", nino, fields, furtherDetailsScopes)

    Scenario("Fetch Self Assessment further details income returns") {

      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        incomeSaSingle
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": [
               |      {
               |        "taxYear": "2019-20",
               |        "furtherDetails": [
               |          {
               |            "busStartDate": "2020-01-01",
               |            "busEndDate": "2020-01-30",
               |            "totalTaxPaid": 100.01,
               |            "totalNIC": 100.01,
               |            "turnover": 100.01,
               |            "otherBusIncome": 100.01,
               |            "tradingIncomeAllowance": 100.01,
               |            "deducts": {
               |              "totalBusExpenses": 200,
               |              "totalDisallowBusExp": 200
               |            }
               |          }
               |        ]
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Fetch Self Assessment further details income returns no root data") {

      val toTaxYear = "2021"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment no data for the user")
      IfStub.searchSaIncomeReturnsNoIncomeFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21")
          .headers(headers)
          .asString

      Then("The response status should be 200 with the self-assessments")
      response.code shouldBe OK
      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links": {
               |    "self": {
               |      "href": "/individuals/income/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2020-21"
               |    }
               |  },
               |  "selfAssessment": {
               |    "taxReturns": []
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Invalid token") {

      Given("A token WITHOUT the required scopes")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("I request the self assessments")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")

    }

    Scenario("The self assessment data source is rate limited") {

      val toTaxYear = "2020"
      Given("A privileged Auth bearer token with the required scopes")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      IfStub.searchSaIncomeReturnsRateLimitErrorFor(
        nino,
        fromTaxYear,
        toTaxYear,
        fields
      )

      When("I request the resources")
      val response =
        Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2019-20")
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

  def testAuthorisation(endpoint:String, scopes: List[String]): Unit = {
    Scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, scopes)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )
    }

    Scenario(s"user does not have valid scopes") {
      Given("A valid auth token but invalid scopes")
      AuthStub.willNotAuthorizePrivilegedAuthTokenNoScopes(authToken)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "UNAUTHORIZED",
        "message" ->"Insufficient Enrolments"
      )
    }
  }

  def testErrorHandling( endpoint: String,
                         nino: String,
                         fields: String,
                         rootScope: List[String]): Unit = {

    Scenario(s"valid request but invalid IF response") {

      Given("A valid auth token ")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("IF will return invalid response")
      IfStub.searchSaIncomeForPeriodReturns(
        nino,
        fromTaxYear,
        toTaxYear,
        fields,
        invalidIncomeSaSingle
      )

      When(
        s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")

      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500 with a generic error message")
      response.code shouldBe INTERNAL_SERVER_ERROR
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "message" -> "Something went wrong.")
    }

    Scenario(s"IF returns an Internal Server Error") {

      Given("A valid auth token ")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("IF will return Internal Server Error")
      IfStub.saCustomResponse(nino, INTERNAL_SERVER_ERROR, fromTaxYear, toTaxYear, fields, Json.obj("reason" -> "Server error"))

      When(
        s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500 with a generic error message")
      response.code shouldBe INTERNAL_SERVER_ERROR
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "message" -> "Something went wrong.")
    }

    Scenario(s"IF returns an Bad Request Error") {

      Given("A valid auth token ")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId, nino)

      And("IF will return Internal Server Error")
      IfStub.saCustomResponse(nino, UNPROCESSABLE_ENTITY, fromTaxYear,  toTaxYear, fields, Json.obj("reason" ->
        "There are 1 or more unknown data items in the 'fields' query string"))

      When(
        s"I make a call to ${if (endpoint.isEmpty) "root" else endpoint} endpoint")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 500 with a generic error message")
      response.code shouldBe INTERNAL_SERVER_ERROR
      Json.parse(response.body) shouldBe Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "message" -> "Something went wrong.")
    }
  }

  def testMatchId(endpoint: String, rootScopes: List[String]): Unit = {

    Scenario("missing match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the API is invoked with a missing match id")

      val response = Http(s"$serviceUrl/$endpoint?fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId is required"
      )

    }

    Scenario("malformed match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the API is invoked with a malformed match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=malformed-id&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (bad request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "matchId format is invalid"
      )

    }

    Scenario("invalid match id") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the paye endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 404 (not found)")
      response.code shouldBe NOT_FOUND
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "NOT_FOUND",
        "message" -> "The resource can not be found"
      )

    }
  }

  def testTaxYears(endpoint: String, rootScopes: List[String]): Unit = {
    Scenario("missing fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear is required"
      )

    }

    Scenario("fromTaxYear earlier than toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2018-19&toTaxYear=2016-17")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "Invalid time period requested"
      )

    }

    Scenario("From date requested is earlier than 31st March 2013") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2012-13&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear earlier than allowed (CY-6)"
      )

    }

    Scenario("Invalid fromTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=201632A-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "fromTaxYear: invalid tax year format"
      )

    }

    Scenario("Invalid toTaxYear") {

      Given("a valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      When("the endpoint is invoked with an invalid match id")
      val response = Http(s"$serviceUrl/$endpoint?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018GFR-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 400 (invalid request)")
      response.code shouldBe BAD_REQUEST
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "INVALID_REQUEST",
        "message" -> "toTaxYear: invalid tax year format"
      )

    }
  }

  private def headers: Map[String, String] = requestHeaders(acceptHeaderP2) + ("X-Client-ID" -> clientId)
}
