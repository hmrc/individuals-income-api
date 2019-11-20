/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.SandboxRootController
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.individualsincomeapi.services.SandboxCitizenMatchingService
import utils.SpecBase

import scala.concurrent.Future.{failed, successful}

class RootControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup {
    val fakeRequest = FakeRequest()
    val matchId = UUID.randomUUID()
    val nino = Nino("NA000799C")
    val matchedCitizen = MatchedCitizen(matchId, nino)
    val mockSandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]
    val mockAuthConnector = mock[AuthConnector]

    val sandboxController = new SandboxRootController(mockSandboxCitizenMatchingService, mockAuthConnector)
  }

  "sandbox match citizen function" should {

    "return 200 (OK) for a valid matchId" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier])).willReturn(successful(matchedCitizen))

      val result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe
        Json.parse(
          s"""
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
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier])).willReturn(failed(new MatchNotFoundException))

      val result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse("""{"code" : "NOT_FOUND", "message" : "The resource can not be found"}""")
    }

    "not require bearer token authentication" in new Setup {
      given(mockSandboxCitizenMatchingService.matchCitizen(refEq(matchId))(any[HeaderCarrier])).willReturn(successful(matchedCitizen))

      val result = await(sandboxController.root(matchId)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }

  }
}
