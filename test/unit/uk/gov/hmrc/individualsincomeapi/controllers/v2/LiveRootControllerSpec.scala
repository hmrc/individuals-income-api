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
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.v2.LiveRootController
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.individualsincomeapi.services.{LiveCitizenMatchingService, ScopesService}
import utils.{AuthHelper, SpecBase}

import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class LiveRootControllerSpec extends SpecBase with AuthHelper with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup {
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector: AuthConnector = fakeAuthConnector(Future.successful(enrolments))
    lazy val scopeService: ScopesService = mock[ScopesService]
    implicit val ec: ExecutionContext = fakeApplication.injector.instanceOf[ExecutionContext]

    val liveMatchCitizenController =
      new LiveRootController(mockLiveCitizenMatchingService, scopeService, mockAuthConnector, cc)

    when(scopeService.getAllScopes).thenReturn(List("hello-world"))
    given(scopeService.getEndPointScopes(any())).willReturn(Seq("hello-world"))

    implicit val hc = HeaderCarrier()
  }

  "Live match citizen controller match citizen function" should {

    val randomMatchId = UUID.randomUUID()

    "return a 500 when a match id does not match live data" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(refEq(randomMatchId))(any[HeaderCarrier]))
        .thenReturn(failed(new MatchNotFoundException))
      val result = intercept[Exception] { await(liveMatchCitizenController.root(randomMatchId).apply(FakeRequest())) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return a 500 when a match id matches live data" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(refEq(randomMatchId))(any[HeaderCarrier]))
        .thenReturn(successful(MatchedCitizen(randomMatchId, Nino("AB123456C"))))

      val result = intercept[Exception] { await(liveMatchCitizenController.root(randomMatchId).apply(FakeRequest())) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "fail with AuthorizedException when the bearer token does not have enrolment read:individuals-income" in new Setup {
      val result = intercept[Exception] { await(liveMatchCitizenController.root(randomMatchId).apply(FakeRequest())) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

  }

}
