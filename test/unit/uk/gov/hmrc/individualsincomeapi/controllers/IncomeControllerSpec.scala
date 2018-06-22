/*
 * Copyright 2018 HM Revenue & Customs
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
import org.joda.time.{Interval, LocalDate}
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.individualsincomeapi.actions.{LivePrivilegedAction, SandboxPrivilegedAction}
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.controllers.{LiveIncomeController, SandboxIncomeController}
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters.paymentJsonFormat
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxMatchId
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, Payment}
import uk.gov.hmrc.individualsincomeapi.services.{LiveIncomeService, SandboxIncomeService}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future.{failed, successful}

class IncomeControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val fromDateString = "2017-03-02"
  val toDateString = "2017-05-31"
  val interval = new Interval(new LocalDate(fromDateString).toDateTimeAtStartOfDay, new LocalDate(toDateString).toDateTimeAtStartOfDay)
  val payments = Seq(Payment(1000.50, LocalDate.parse("2016-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10)))

  trait Setup {
    val mockIncomeService: LiveIncomeService = mock[LiveIncomeService]
    val mockAuthConnector: ServiceAuthConnector = mock[ServiceAuthConnector]
    val testLivePrivilegedAction = new LivePrivilegedAction(mockAuthConnector)
    val testSandboxPrivilegedAction = new SandboxPrivilegedAction()

    val liveIncomeController = new LiveIncomeController(mockIncomeService, testLivePrivilegedAction)
    val sandboxIncomeController = new SandboxIncomeController(new SandboxIncomeService, testSandboxPrivilegedAction)

    given(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))
  }

  "Income controller income function" should {
    val fakeRequest = FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString&toDate=$toDateString")

    "return 200 (OK) when matching succeeds and service returns payments" in new Setup {
      given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any())).willReturn(payments)

      val result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(fakeRequest.uri))
    }

    "return 200 (OK) when matching succeeds and service returns no payments" in new Setup {
      given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any())).willReturn(Seq.empty)

      val result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(fakeRequest.uri, Seq.empty))
    }

    "return 200 (OK) with correct self link response when toDate is not provided in the request" in new Setup {
      val fakeRequest = FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString")

      given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any())).willReturn(payments)

      val result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(fakeRequest.uri))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any())).willReturn(failed(new MatchNotFoundException()))

      val result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse( s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      val result = await(sandboxIncomeController.income(sandboxMatchId, interval)(fakeRequest))

      status(result) shouldBe OK
      verifyZeroInteractions(mockAuthConnector)
    }

  }

  private def expectedPayload(uri: String, payments: Seq[Payment] = payments) = {
    s"""
       {
         "_links": {
           "self": {"href": "$uri"}
         },
         "paye": {
           "income": ${Json.toJson(payments)}
         }
       }
      """
  }
}
