/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPaye
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxMatchId
import utils.IncomePayeHelpers

class IndividualIncomeSpec extends CommonControllerSpec with IncomePayeHelpers {

  val matchId = UUID.randomUUID().toString
  val nino = "CS700100A"
  val fromDate = "2019-04-01"
  val toDate = "2020-01-01"
  val endpoint = "paye"
  val incomePayeSingle = IfPaye(Seq(createValidPayeEntry()))

  val fields =
    "paye(dednsFromNetPay,employedPayeRef,employee(hasPartner),employeeNICs(inPayPeriod1,inPayPeriod2," +
      "inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4),employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4),monthlyPeriodNumber,paidHoursWorked," +
      "payFrequency,paymentDate,payroll(id),statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
      "taxCode,taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
      "totalEmployerNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "totalTaxToDate,weeklyPeriodNumber)"

  val rootScope = List(
    "read:individuals-income-hmcts-c2",
    "read:individuals-income-hmcts-c3",
    "read:individuals-income-hmcts-c4",
    "read:individuals-income-laa-c1",
    "read:individuals-income-laa-c2",
    "read:individuals-income-laa-c3",
    "read:individuals-income-laa-c4",
    "read:individuals-income-lsani-c1",
    "read:individuals-income-lsani-c3",
    "read:individuals-income-nictsejo-c4"
  )

  val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
  val sampleCorrelationIdHeader = "CorrelationId" -> sampleCorrelationId

  Feature("Live individual income") {

    Scenario("not authorized") {

      Given("an invalid privileged Auth bearer token")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, rootScope)

      When("the API is invoked")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("the response status should be 401 (unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "UNAUTHORIZED",
        "message" -> "Bearer token is missing or not authorized"
      )

    }

    Scenario("Individual has employment income") {

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

      And("IF will return income data for the NINO")
      IfStub.searchPayeIncomeForPeriodReturns(
        nino,
        fromDate,
        toDate,
        fields,
        incomePayeSingle
      )

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 200")
      response.code shouldBe OK

      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links":{
               |    "self":{
               |      "href":"/individuals/income/paye?matchId=$matchId&fromDate=2019-04-01&toDate=2020-01-01"
               |    }
               |  },
               |  "paye":{
               |    "income":[
               |      {
               |        "employerPayeReference":"345/34678",
               |        "taxYear":"18-19",
               |        "employee":{
               |          "hasPartner": false
               |        },
               |        "payroll": {
               |          "id": "yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"
               |        },
               |        "payFrequency":"W4",
               |        "monthPayNumber": 3,
               |        "weekPayNumber": 2,
               |        "paymentDate":"2006-02-27",
               |        "paidHoursWorked":"36",
               |        "taxCode":"K971",
               |        "taxablePayToDate":19157.5,
               |        "taxablePay":16533.95,
               |        "totalTaxToDate":3095.89,
               |        "taxDeductedOrRefunded":159228.49,
               |        "dednsFromNetPay":198035.8,
               |        "employeePensionContribs":{
               |          "paidYTD":169731.51,
               |          "notPaidYTD":173987.07,
               |          "paid":822317.49,
               |          "notPaid":818841.65
               |        },
               |        "statutoryPayYTD":{
               |          "maternity":15797.45,
               |          "paternity":13170.69,
               |          "adoption":16193.76,
               |          "parentalBereavement":30846.56
               |        },
               |        "grossEarningsForNics":{
               |          "inPayPeriod1":169731.51,
               |          "inPayPeriod2":173987.07,
               |          "inPayPeriod3":822317.49,
               |          "inPayPeriod4":818841.65
               |        },
               |        "totalEmployerNics":{
               |          "inPayPeriod1":15797.45,
               |          "inPayPeriod2":13170.69,
               |          "inPayPeriod3":16193.76,
               |          "inPayPeriod4":30846.56,
               |          "ytd1":10633.5,
               |          "ytd2":15579.18,
               |          "ytd3":110849.27,
               |          "ytd4":162081.23
               |        },
               |        "employeeNics":{
               |          "inPayPeriod1":15797.45,
               |          "inPayPeriod2":13170.69,
               |          "inPayPeriod3":16193.76,
               |          "inPayPeriod4":30846.56,
               |          "ytd1":10633.5,
               |          "ytd2":15579.18,
               |          "ytd3":110849.27,
               |          "ytd4":162081.23
               |        }
               |      }
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("Individual has no paye income") {
      val toDate = "2020-02-01"

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

      And("IF will return paye for the NINO")
      IfStub.searchPayeIncomeReturnsNoIncomeFor(
        nino,
        fromDate,
        toDate,
        fields
      )

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 200")
      response.code shouldBe OK

      response.body shouldBe
        Json
          .parse(
            s"""{
               |  "_links":{
               |    "self":{
               |      "href":"/individuals/income/paye?matchId=$matchId&fromDate=2019-04-01&toDate=$toDate"
               |    }
               |  },
               |  "paye":{
               |    "income":[
               |    ]
               |  }
               |}""".stripMargin
          )
          .toString()
    }

    Scenario("The employment income data source is rate limited") {
      val toDate = "2020-02-02"

      Given("A valid privileged Auth bearer token")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScope)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.hasMatchFor(matchId.toString, nino)

      And("IF is rate limited")
      IfStub.searchPayeIncomeReturnsRateLimitErrorFor(
        nino,
        fromDate,
        toDate,
        fields
      )

      When("I request individual income for the existing matchId")
      val response = Http(s"$serviceUrl/paye?matchId=$matchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 429 Too Many Requests")
      response.code shouldBe TOO_MANY_REQUESTS
      Json.parse(response.body) shouldBe Json.obj(
        "code"    -> "TOO_MANY_REQUESTS",
        "message" -> "Rate limit exceeded"
      )
    }
  }

  Feature("Sandbox individual income") {

    Scenario("Valid request to the sandbox implementation") {

      When("I request individual income for the sandbox matchId")
      val response = Http(s"$serviceUrl/sandbox/paye?matchId=$sandboxMatchId&fromDate=$fromDate&toDate=$toDate")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 200")
      response.code shouldBe OK

      response.body shouldBe
        Json
          .parse(
            s"""{"_links":{
               |"self":{"href":"/individuals/income/paye?matchId=57072660-1df9-4aeb-b4ea-cd2d7f96e430&fromDate=2019-04-01&toDate=2020-01-01"}},
               |"paye":{"income":[{
               |  "employerPayeReference":"345/34678",
               |  "taxYear":"18-19",
               |  "employee":{
               |    "hasPartner": false
               |  },
               |  "payroll": {
               |    "id": "yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"
               |  },
               |  "payFrequency":"W4",
               |  "paymentDate":"2019-05-27",
               |  "paidHoursWorked":"36",
               |  "taxCode":"K971",
               |  "taxablePayToDate":19157.5,
               |  "totalTaxToDate":3095.89,
               |  "taxDeductedOrRefunded":159228.49,
               |  "dednsFromNetPay":198035.8,
               |  "employeePensionContribs":{
               |    "paidYTD":169731.51,
               |    "notPaidYTD":173987.07,
               |    "paid":822317.49,
               |    "notPaid":818841.65
               |  },
               |  "statutoryPayYTD":{
               |    "maternity":15797.45,
               |    "paternity":13170.69,
               |    "adoption":16193.76,
               |    "parentalBereavement":30846.56
               |  },
               |  "grossEarningsForNics":{
               |    "inPayPeriod1":169731.51,
               |    "inPayPeriod2":173987.07,
               |    "inPayPeriod3":822317.49,
               |    "inPayPeriod4":818841.65},
               |    "totalEmployerNics":{
               |      "inPayPeriod1":15797.45,
               |      "inPayPeriod2":13170.69,
               |      "inPayPeriod3":16193.76,
               |      "inPayPeriod4":30846.56,
               |      "ytd1":10633.5,
               |      "ytd2":15579.18,
               |      "ytd3":110849.27,
               |      "ytd4":162081.23
               |    },"employeeNics":{
               |      "inPayPeriod1":15797.45,
               |      "inPayPeriod2":13170.69,
               |      "inPayPeriod3":16193.76,
               |      "inPayPeriod4":30846.56,
               |      "ytd1":10633.5,
               |      "ytd2":15579.18,
               |      "ytd3":110849.27,
               |      "ytd4":162081.23
               |    }
               |  }
               |]
               |}}""".stripMargin
          )
          .toString()
    }
  }
}
