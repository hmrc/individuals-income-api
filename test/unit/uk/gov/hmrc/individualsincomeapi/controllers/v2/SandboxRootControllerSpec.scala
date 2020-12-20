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
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.v2.{LiveIncomeController, LiveRootController, SandboxIncomeController, SandboxRootController}
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveIncomeService, SandboxIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, SpecBase}
import play.api.http.Status._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.{failed, successful}

class SandboxRootControllerSpec extends SpecBase with AuthHelper with MockitoSugar with ScalaFutures {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup extends ScopesConfigHelper {

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

      val result = await(sandboxRootController.root(matchId)(fakeRequest))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe
        Json.parse(s"""
         {"_links":{
           "incomePaye":{
             "href":"/individuals/income/paye?matchId=$matchId{&startDate,endDate}",
             "title":"Get an individual's income paye data"
             },
             "self":{
               "href":"/individuals/income/?matchId=$matchId"
             }
           }
         }""")
    }

    "return 400 for an invalid matchId" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(failed(new MatchNotFoundException))

      val result = await(sandboxRootController.root(matchId)(fakeRequest))

      status(result) shouldBe NOT_FOUND

      jsonBodyOf(result) shouldBe Json.parse(
        """{"code" : "NOT_FOUND", "message" : "The resource can not be found"}"""
      )
    }

    "not require bearer token authentication" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier]))
        .willReturn(successful(matchedCitizen))

      val result = await(sandboxRootController.root(matchId)(fakeRequest))

      status(result) shouldBe OK
      verifyNoInteractions(mockAuthConnector)
    }

  }
}
