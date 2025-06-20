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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v1

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{times, verify, verifyNoInteractions}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v1.SandboxRootController
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import uk.gov.hmrc.individualsincomeapi.services.SandboxCitizenMatchingService
import utils.SpecBase

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.{failed, successful}

class RootControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {
  implicit lazy val materializer: Materializer = fakeApplication().materializer

  trait Setup {
    val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val matchId: UUID = UUID.randomUUID()
    val nino: Nino = Nino("NA000799C")
    val matchedCitizen: MatchedCitizen = MatchedCitizen(matchId, nino)
    implicit val ec: ExecutionContext = fakeApplication().injector.instanceOf[ExecutionContext]
    val mockSandboxCitizenMatchingService: SandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    val sandboxController =
      new SandboxRootController(mockSandboxCitizenMatchingService, mockAuthConnector, cc, mockAuditHelper)
  }

  "sandbox match citizen function" should {

    "return 200 (OK) for a valid matchId" in new Setup {
      `given`(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val result: Result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe
        Json.parse(s"""
         {
             "_links": {
                 "paye": {
                     "href": "/individuals/income/paye?matchId=$matchId{&fromDate,toDate}",
                     "title": "View individual's income per employment"
                 },
                 "selfAssessment": {
                    "href": "/individuals/income/sa?matchId=$matchId{&fromTaxYear,toTaxYear}",
                    "title": "View individual's self-assessment income"
                 },
                 "self": {
                     "href": "/individuals/income/?matchId=$matchId"
                 }
             }
         }""")
    }

    "return 400 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(failed(new MatchNotFoundException))

      val result: Result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse("""{"code" : "NOT_FOUND", "message" : "The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return 500 (Internal Server Error) for an invalid matchId" in new Setup {
      `given`(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(failed(new Exception()))

      val result: Result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "INTERNAL_SERVER_ERROR", "message" : "Something went wrong."}"""
      )
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "not require bearer token authentication" in new Setup {
      `given`(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val result: Result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe OK
      verifyNoInteractions(mockAuthConnector)
    }

  }
}
