/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.Mockito.{times, verify, verifyNoInteractions}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v2.RootController
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import uk.gov.hmrc.individualsincomeapi.services.LiveCitizenMatchingService
import uk.gov.hmrc.individualsincomeapi.services.v2.{IncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, SpecBase}

import java.util.UUID
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class LiveRootControllerSpec extends SpecBase with AuthHelper with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication().materializer

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader: (String, String) = "CorrelationId" -> sampleCorrelationId

    val controllerComponent: ControllerComponents = fakeApplication().injector.instanceOf[ControllerComponents]
    val mockLiveIncomeService: IncomeService = mock[IncomeService]
    val mockLiveCitizenMatchingService: LiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    implicit lazy val ec: ExecutionContext = fakeApplication().injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(sampleCorrelationIdHeader)
    val matchId: UUID = UUID.randomUUID()
    val matchIdString: String = matchId.toString
    val nino: Nino = Nino("NA000799C")
    val matchedCitizen: MatchedCitizen = MatchedCitizen(matchId, nino)

    val liveRootController = new RootController(
      mockLiveCitizenMatchingService,
      scopeService,
      scopesHelper,
      mockAuthConnector,
      mockAuditHelper,
      controllerComponent
    )

    implicit val hc: HeaderCarrier = HeaderCarrier()

    mockAuthConnector.authorise(*, Retrievals.allEnrolments)(*, *) returns
      Future.successful(Enrolments(Set(Enrolment("test-scope"), Enrolment("test-scope-1"))))

  }

  "Live match citizen controller match citizen function" should {

    val randomMatchId = UUID.randomUUID()

    "return a 404 when a match id does not match live data" in new Setup {
      mockLiveCitizenMatchingService.matchCitizen(randomMatchId)(any[HeaderCarrier]) returns failed(
        new MatchNotFoundException
      )

      val result: Result =
        await(
          liveRootController.root(randomMatchId.toString).apply(FakeRequest().withHeaders(sampleCorrelationIdHeader))
        )

      status(result) shouldBe NOT_FOUND

      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "NOT_FOUND", "message" : "The resource can not be found"}"""
      )

      verify(liveRootController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return a 200 when a match id matches live data" in new Setup {

      mockLiveCitizenMatchingService.matchCitizen(matchId)(any[HeaderCarrier]) returns successful(matchedCitizen)

      val result: Result =
        await(liveRootController.root(matchId.toString).apply(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

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

      verify(liveRootController.auditHelper, times(1)).auditApiResponse(*, *, *, *, *, *)(*)
    }

    "fail with AuthorizedException when the bearer token does not have valid scopes" in new Setup {

      mockAuthConnector.authorise(*, Retrievals.allEnrolments)(*, *) returns Future.failed(InsufficientEnrolments())

      val result: Result =
        await(
          liveRootController.root(randomMatchId.toString).apply(FakeRequest().withHeaders(sampleCorrelationIdHeader))
        )

      status(result) shouldBe UNAUTHORIZED

      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveCitizenMatchingService)

      verify(liveRootController.auditHelper, times(1)).auditApiFailure(*, *, *, *, *)(*)
    }

    "returns bad request with correct message when missing CorrelationId" in new Setup {

      override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      mockLiveCitizenMatchingService.matchCitizen(matchId)(any[HeaderCarrier]) returns successful(matchedCitizen)

      val result: Result = await(liveRootController.root(matchId.toString)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(liveRootController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "throws an exception when malformed CorrelationId" in new Setup {
      override val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest().withHeaders("CorrelationId" -> "test")

      mockLiveCitizenMatchingService.matchCitizen(matchId)(any[HeaderCarrier]) returns successful(matchedCitizen)

      val result: Result = await(liveRootController.root(matchIdString)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(liveRootController.auditHelper, times(1)).auditApiFailure(*, *, *, *, *)(*)
    }

  }

}
