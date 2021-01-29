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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq, eq => eqTo}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verifyNoInteractions
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.individualsincomeapi.controllers.v2.SandboxRootController
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import uk.gov.hmrc.individualsincomeapi.services.SandboxCitizenMatchingService
import uk.gov.hmrc.individualsincomeapi.services.v2.{SandboxIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, SpecBase}

import java.util.UUID
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class SandboxRootControllerSpec extends SpecBase with AuthHelper with MockitoSugar with ScalaFutures {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader = ("CorrelationId" -> sampleCorrelationId)

    val controllerComponent = fakeApplication.injector.instanceOf[ControllerComponents]
    val mockSandboxIncomeService = mock[SandboxIncomeService]
    val mockSandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val fakeRequest = FakeRequest()
    val matchId = UUID.randomUUID()
    val nino = Nino("NA000799C")
    val matchedCitizen = MatchedCitizen(matchId, nino)

    val sandboxRootController = new SandboxRootController(
      mockSandboxCitizenMatchingService,
      scopeService,
      scopesHelper,
      mockAuthConnector,
      controllerComponent)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockAuthConnector.authorise(eqTo(Enrolment("test-scope")), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  "sandbox match citizen function" should {

    "return 200 for a valid matchId" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val result = await(sandboxRootController.root(matchId)(fakeRequest.withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe
        Json.parse(s"""{
                      |  "_links": {
                      |    "sa": {
                      |      "href": "/individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
                      |      "title": "Get an individual's income sa data"
                      |    },
                      |    "paye": {
                      |      "href": "/individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
                      |      "title": "Get an individual's income paye data"
                      |    },
                      |    "self": {
                      |      "href": "/individuals/income/?matchId=$matchId"
                      |    }
                      |  }
                      |}""".stripMargin)
    }

    "return 400 for an invalid matchId" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(failed(new MatchNotFoundException))

      val result = await(sandboxRootController.root(matchId)(fakeRequest.withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "NOT_FOUND", "message" : "The resource can not be found"}"""
      )
    }

    "not require bearer token authentication" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val result = await(sandboxRootController.root(matchId)(fakeRequest.withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe OK
      verifyNoInteractions(mockAuthConnector)
    }

    "throws an exception when CorrelationId is missing" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val exception = intercept[BadRequestException](sandboxRootController.root(matchId)(fakeRequest))

      exception.message shouldBe "CorrelationId is required"
      exception.responseCode shouldBe BAD_REQUEST
    }

    "throws an exception when CorrelationId is Malformed" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val exception = intercept[BadRequestException](
        sandboxRootController.root(matchId)(fakeRequest.withHeaders("CorrelationId" -> "test")))

      exception.message shouldBe "Malformed CorrelationId"
      exception.responseCode shouldBe BAD_REQUEST
    }
  }
}
