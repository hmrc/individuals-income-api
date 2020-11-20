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

import java.util.UUID

import component.uk.gov.hmrc.individualsincomeapi.stubs.{AuthStub, BaseSpec, DesStub, IndividualsMatchingApiStub}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.test.Helpers._
import scalaj.http.Http
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.individualsincomeapi.domain.{DesSAIncome, DesSAReturn, SAIncome, TaxYear}

class LiveSaIncomeControllerSpec extends BaseSpec {

  val matchId = UUID.randomUUID().toString
  val nino = Nino("AA100009B")
  val fromTaxYear = TaxYear("2016-17")
  val toTaxYear = TaxYear("2018-19")

  feature("SA root endpoint") {

    val rootScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-hmcts-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-laa-c4",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment annual returns") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      // TODO: Fill in

      When("I request the sa root resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the self-assessments")
      val requestParameters = s"matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19"

      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa")
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

    scenario("The self assessment data source is rate limited") {
      val toTaxYear = TaxYear("2017-18")

      Given("A privileged Auth bearer token with scope read:individuals-income-sa")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, rootScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF is rate limited")
      // TODO: Fill in

      When("I request the sa root resources")
      val response = Http(s"$serviceUrl/sa?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2017-18")
        .headers(headers)
        .asString

      Then("The response status should be 500")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }
  }

  feature("SA employments endpoint") {

    val employmentScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment employments") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-employments")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      // TODO: Fill in

      When("I request the employments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the employments")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-employments")
      AuthStub
        .willNotAuthorizePrivilegedAuthToken(authToken, employmentScopes)

      When("I request the employments")
      val response = Http(s"$serviceUrl/sa/employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA self employments endpoint") {

    val selfAssessmentScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment self employments") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-self-employments")
      AuthStub
        .willAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the user")
      // TODO: Fill in

      When("I request the self employments")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the self employments")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-self-employments")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, selfAssessmentScopes)

      When("I request the self employments")
      val response = Http(s"$serviceUrl/sa/self-employments?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA summary endpoint") {

    val summaryScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment summary") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-summary")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa summary")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the self employments")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-summary")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, summaryScopes)

      When("I request the sa summary")
      val response = Http(s"$serviceUrl/sa/summary?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA trusts endpoint") {

    val trustsScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment trusts income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-trusts")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa trusts income")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the trusts income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-trusts")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, trustsScopes)

      When("I request the sa trusts income")
      val response = Http(s"$serviceUrl/sa/trusts?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA foreign endpoint") {

    val foreignScopes = List(
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment foreign income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-foreign")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa foreign income")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the foreign income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-foreign")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, foreignScopes)

      When("I request the sa foreign income")
      val response = Http(s"$serviceUrl/sa/foreign?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA partnerships endpoint") {

    val partnershipsScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment partnerships income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-partnerships")
      AuthStub
        .willAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa partnerships income")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the foreign income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-partnerships")
      AuthStub
        .willNotAuthorizePrivilegedAuthToken(authToken, partnershipsScopes)

      When("I request the sa partnerships income")
      val response = Http(s"$serviceUrl/sa/partnerships?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA interests and dividends income") {

    val interestsAndDividendsScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment interests and dividends income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-interests-and-dividends")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa interests and dividends income")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 500 with the foreign income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-interests-and-dividends")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, interestsAndDividendsScopes)

      When("I request the sa interests and dividends income")
      val response =
        Http(s"$serviceUrl/sa/interests-and-dividends?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA pensions and state benefits income") {

    val pensionsAndStateBenefitsScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment pensions and state benefits income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-pensions-and-state-benefits")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa pensions and state benefits income")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 500 with the foreign income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-pensions-and-state-benefits")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, pensionsAndStateBenefitsScopes)

      When("I request the sa pensions and state benefits income")
      val response =
        Http(s"$serviceUrl/sa/pensions-and-state-benefits?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA UK properties income") {

    val ukPropertiesScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-hmcts-c2",
      "read:individuals-income-hmcts-c3",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment UK properties income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-uk-properties")
      AuthStub
        .willAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa UK properties income income")
      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the UK properties income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-uk-properties")
      AuthStub
        .willNotAuthorizePrivilegedAuthToken(authToken, ukPropertiesScopes)

      When("I request the sa UK properties income")
      val response = Http(s"$serviceUrl/sa/uk-properties?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA additional information") {

    val additionalInformationScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment additional information") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-additional-information")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa additional information")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(headers)
          .asString

      Then("The response status should be 500 with the UK properties income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-additional-information")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, additionalInformationScopes)

      When("I request the sa additional information")
      val response =
        Http(s"$serviceUrl/sa/additional-information?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
          .headers(requestHeaders(acceptHeaderP2))
          .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA other income") {

    val otherIncomeScopes = List(
      "read:individuals-employments-nictsejo-c4",
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment other income") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-other")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa other income")
      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the other income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-other")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, otherIncomeScopes)

      When("I request the sa other income")
      val response = Http(s"$serviceUrl/sa/other?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  feature("SA further details") {

    val furtherDetailsScopes = List(
      "read:individuals-income-laa-c1",
      "read:individuals-income-laa-c2",
      "read:individuals-income-laa-c3",
      "read:individuals-income-lsani-c1",
      "read:individuals-income-lsani-c3"
    )

    scenario("Fetch Self Assessment further details") {
      Given("A privileged Auth bearer token with scope read:individuals-income-sa-further-details")
      AuthStub.willAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      And("a valid record in the matching API")
      IndividualsMatchingApiStub.willRespondWith(matchId, OK, s"""{"matchId" : "$matchId", "nino" : "$nino"}""")

      And("IF will return self-assessment data for the individual")
      // TODO: Fill in

      When("I request the sa further details")
      val response = Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(headers)
        .asString

      Then("The response status should be 500 with the other income")
      response.code shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "{\"statusCode\":500,\"message\":\"NOT_IMPLEMENTED\"}"
    }

    scenario("Invalid token") {
      Given("A token WITHOUT the scope read:individuals-income-sa-further-details")
      AuthStub.willNotAuthorizePrivilegedAuthToken(authToken, furtherDetailsScopes)

      When("I request the sa further details")
      val response = Http(s"$serviceUrl/sa/further-details?matchId=$matchId&fromTaxYear=2016-17&toTaxYear=2018-19")
        .headers(requestHeaders(acceptHeaderP2))
        .asString

      Then("The response status should be 401 (Unauthorized)")
      response.code shouldBe UNAUTHORIZED
      Json.parse(response.body) shouldBe Json
        .obj("code" -> "UNAUTHORIZED", "message" -> "Bearer token is missing or not authorized")
    }
  }

  private def headers: Map[String, String] = requestHeaders(acceptHeaderP2) + ("X-Client-ID" -> clientId)
}
