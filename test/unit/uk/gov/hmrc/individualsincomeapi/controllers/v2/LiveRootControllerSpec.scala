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

import java.util.UUID
import akka.stream.Materializer
import org.mockito.ArgumentMatchers.refEq
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test._
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.individualsincomeapi.controllers.v2.{LiveRootController, SandboxRootController}
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveIncomeService, SandboxIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, SpecBase}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class LiveRootControllerSpec extends SpecBase with AuthHelper with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader = ("CorrelationId" -> sampleCorrelationId)

    val controllerComponent = fakeApplication.injector.instanceOf[ControllerComponents]
    val mockLiveIncomeService = mock[LiveIncomeService]
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val fakeRequest = FakeRequest().withHeaders(sampleCorrelationIdHeader)
    val matchId = UUID.randomUUID()
    val nino = Nino("NA000799C")
    val matchedCitizen = MatchedCitizen(matchId, nino)

    val liveRootController = new LiveRootController(
      mockLiveCitizenMatchingService,
      scopeService,
      scopesHelper,
      mockAuthConnector,
      controllerComponent)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockAuthConnector.authorise(any(), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope"), Enrolment("test-scope-1")))))
  }

  "Live match citizen controller match citizen function" should {

    val randomMatchId = UUID.randomUUID()

    "return a 404 when a match id does not match live data" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(refEq(randomMatchId))(any[HeaderCarrier]))
        .thenReturn(failed(new MatchNotFoundException))

      val result =
        await(liveRootController.root(randomMatchId).apply(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "NOT_FOUND", "message" : "The resource can not be found"}"""
      )
    }

    "return a 200 when a match id matches live data" in new Setup {

      when(mockLiveCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .thenReturn(successful(matchedCitizen))

      val result = await(liveRootController.root(matchId).apply(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe
        Json.parse(s"""{
                      |  "_links": {
                      |    "incomeSa": {
                      |      "href": "/individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
                      |      "title": "Get an individual's income sa data"
                      |    },
                      |    "incomePaye": {
                      |      "href": "/individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
                      |      "title": "Get an individual's income paye data"
                      |    },
                      |    "self": {
                      |      "href": "/individuals/income/?matchId=$matchId"
                      |    }
                      |  }
                      |}""".stripMargin)
    }

    "fail with AuthorizedException when the bearer token does not have valid scopes" in new Setup {

      given(mockAuthConnector.authorise(any(), refEq(Retrievals.allEnrolments))(any(), any()))
        .willReturn(Future.failed(InsufficientEnrolments()))

      val result =
        await(liveRootController.root(randomMatchId).apply(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe UNAUTHORIZED

      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveCitizenMatchingService)
    }

    "throws an exception when missing CorrelationId" in new Setup {

      override val fakeRequest = FakeRequest()

      when(mockLiveCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .thenReturn(successful(matchedCitizen))

      val exception =
        intercept[BadRequestException](liveRootController.root(matchId)(fakeRequest))

      exception.message shouldBe "CorrelationId is required"
      exception.responseCode shouldBe BAD_REQUEST
    }

    "throws an exception when malformed CorrelationId" in new Setup {
      override val fakeRequest = FakeRequest().withHeaders("CorrelationId" -> "test")

      when(mockLiveCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .thenReturn(successful(matchedCitizen))

      val exception =
        intercept[BadRequestException](liveRootController.root(matchId)(fakeRequest))

      exception.message shouldBe "Malformed CorrelationId"
      exception.responseCode shouldBe BAD_REQUEST
    }

  }

}
