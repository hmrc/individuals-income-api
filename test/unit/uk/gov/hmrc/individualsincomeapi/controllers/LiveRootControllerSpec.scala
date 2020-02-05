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

package unit.uk.gov.hmrc.individualsincomeapi.controllers

import java.util.UUID

import akka.stream.Materializer
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.json.Json.parse
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.LiveRootController
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, MatchedCitizen}
import uk.gov.hmrc.individualsincomeapi.services.LiveCitizenMatchingService
import utils.SpecBase

import scala.concurrent.Future.{failed, successful}

class LiveRootControllerSpec extends SpecBase with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup {
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuthConnector = mock[AuthConnector]

    val liveMatchCitizenController = new LiveRootController(mockLiveCitizenMatchingService, mockAuthConnector, cc)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))
    implicit val hc = HeaderCarrier()
  }

  "Live match citizen controller match citizen function" should {

    val randomMatchId = UUID.randomUUID()

    "return a 404 (not found) when a match id does not match live data" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(refEq(randomMatchId))(any[HeaderCarrier]))
        .thenReturn(failed(new MatchNotFoundException))

      val eventualResult = liveMatchCitizenController.root(randomMatchId).apply(FakeRequest())

      status(eventualResult) shouldBe NOT_FOUND
      contentAsJson(eventualResult) shouldBe parse("""
          {
            "code":"NOT_FOUND",
            "message":"The resource can not be found"
          }
        """)
    }

    "return a 200 (ok) when a match id matches live data" in new Setup {
      when(mockLiveCitizenMatchingService.matchCitizen(refEq(randomMatchId))(any[HeaderCarrier]))
        .thenReturn(successful(MatchedCitizen(randomMatchId, Nino("AB123456C"))))

      val eventualResult = liveMatchCitizenController.root(randomMatchId).apply(FakeRequest())

      status(eventualResult) shouldBe OK
      contentAsJson(eventualResult) shouldBe parse(s"""
          {
            "_links":{
              "paye":{
                "href":"/individuals/income/paye?matchId=$randomMatchId{&fromDate,toDate}",
                "title":"View individual's income per employment"
              },
              "selfAssessment":{
                "href":"/individuals/income/sa?matchId=$randomMatchId{&fromTaxYear,toTaxYear}",
                "title":"View individual's self-assessment income"
              },
              "self":{
                "href":"/individuals/income/?matchId=$randomMatchId"
              }
            }
          }
        """)
    }

    "fail with AuthorizedException when the bearer token does not have enrolment read:individuals-income" in new Setup {
      given(
        mockAuthConnector.authorise(refEq(Enrolment("read:individuals-income")), refEq(EmptyRetrieval))(any(), any()))
        .willReturn(failed(new InsufficientEnrolments()))

      val result = await(liveMatchCitizenController.root(randomMatchId).apply(FakeRequest()))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verifyNoInteractions(mockLiveCitizenMatchingService)
    }

  }

}
