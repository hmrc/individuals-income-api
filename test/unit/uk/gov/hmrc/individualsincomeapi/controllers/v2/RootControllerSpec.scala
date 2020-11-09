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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import java.util.UUID

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.v2.SandboxRootController
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.individualsincomeapi.services.{SandboxCitizenMatchingService, ScopesService}
import utils.{AuthHelper, SpecBase}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future.{failed, successful}

class RootControllerSpec extends SpecBase with AuthHelper with MockitoSugar with ScalaFutures {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup {
    val fakeRequest = FakeRequest()
    val matchId = UUID.randomUUID()
    val nino = Nino("NA000799C")
    val matchedCitizen = MatchedCitizen(matchId, nino)
    implicit val ec: ExecutionContext = fakeApplication.injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = mock[ScopesService]

    val mockSandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]
    val mockAuthConnector = mock[AuthConnector]
    val sandboxController =
      new SandboxRootController(mockSandboxCitizenMatchingService, scopeService, mockAuthConnector, cc)

    given(scopeService.getEndPointScopes(any())).willReturn(Seq("hello-world"))
  }

  "sandbox match citizen function" should {

    "return 200 (OK) for a valid matchId" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val result = intercept[Exception] { await(sandboxController.root(matchId)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 400 (Not Found) for an invalid matchId" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(failed(new MatchNotFoundException))

      val result = intercept[Exception] { await(sandboxController.root(matchId)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "not require bearer token authentication" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))
      val result = intercept[Exception] { await(sandboxController.root(matchId)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

  }
}
