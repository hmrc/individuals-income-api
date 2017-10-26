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

package unit.uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.SandboxSaIncomeController
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.SandboxSaIncomeService
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._

import scala.concurrent.Future.{failed, successful}

class SaIncomeControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  implicit lazy val materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val fromTaxYear = TaxYear("2015-16")
  val toTaxYear = TaxYear("2016-17")
  val taxYearInterval = TaxYearInterval(fromTaxYear, toTaxYear)
  val saReturns = Seq(SaAnnualReturns(TaxYear("2015-16"), Seq(SaReturn(LocalDate.parse("2016-06-01")))))

  trait Setup {
    val mockAuthConnector = mock[ServiceAuthConnector]
    val mockSandboxSaIncomeService = mock[SandboxSaIncomeService]

    val sandboxSaIncomeController = new SandboxSaIncomeController(mockSandboxSaIncomeService, mockAuthConnector)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))
  }

  "SandboxSaIncomeController.saReturns" should {
    val requestParameters = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}&toTaxYear=${toTaxYear.formattedTaxYear}"
    val fakeRequest = FakeRequest("GET", s"/individuals/income/sa?$requestParameters")

    "return 200 (OK) with the self assessment returns for the period" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaReturnsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturns))

      val result = await(sandboxSaIncomeController.saReturns(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(requestParameters))
    }

    "return 200 (Ok) and the links without toTaxYear when it is not passed in the request" in new Setup {
      val requestParametersWithoutToTaxYear = s"matchId=$matchId&fromTaxYear=${fromTaxYear.formattedTaxYear}"
      val fakeRequestWithoutToTaxYear = FakeRequest("GET", s"/individuals/income/sa?$requestParametersWithoutToTaxYear")

      given(mockSandboxSaIncomeService.fetchSaReturnsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturns))

      val result = await(sandboxSaIncomeController.saReturns(matchId, taxYearInterval)(fakeRequestWithoutToTaxYear))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(requestParametersWithoutToTaxYear))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaReturnsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(sandboxSaIncomeController.saReturns(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      given(mockSandboxSaIncomeService.fetchSaReturnsByMatchId(refEq(matchId), refEq(taxYearInterval))(any()))
        .willReturn(successful(saReturns))

      val result = await(sandboxSaIncomeController.saReturns(matchId, taxYearInterval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }

  }

  private def expectedPayload(requestParameters: String) = {
    s"""
       {
         "_links": {
           "self": {"href": "/individuals/income/sa?$requestParameters"},
           "employments": {"href": "/individuals/income/sa/employments?$requestParameters"},
           "self-employments": {"href": "/individuals/income/sa/self-employments?$requestParameters"}
         },
         "_embedded": {
           "income": ${Json.toJson(saReturns)}
         }
       }
      """
  }
}
