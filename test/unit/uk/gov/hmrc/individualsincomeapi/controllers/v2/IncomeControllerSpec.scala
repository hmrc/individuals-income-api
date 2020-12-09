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
import org.joda.time.{Interval, LocalDate}
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito.given
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.individualsincomeapi.controllers.v2.{LiveIncomeController, SandboxIncomeController}
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, Payment}
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveIncomeService, SandboxIncomeService}
import uk.gov.hmrc.individualsincomeapi.services.v2.ScopesService
import utils.{AuthHelper, SpecBase}

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class IncomeControllerSpec extends SpecBase with AuthHelper with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  val matchId = UUID.randomUUID()
  val fromDateString = "2017-03-02"
  val toDateString = "2017-05-31"
  val interval = new Interval(
    new LocalDate(fromDateString).toDateTimeAtStartOfDay,
    new LocalDate(toDateString).toDateTimeAtStartOfDay)
  val payments = Seq(
    Payment(1000.50, LocalDate.parse("2016-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10)))

  trait Setup {
    val mockIncomeService: LiveIncomeService = mock[LiveIncomeService]
    val mockAuthConnector: AuthConnector = fakeAuthConnector(Future.successful(enrolments))
    lazy val scopeService: ScopesService = mock[ScopesService]

    val liveIncomeController = new LiveIncomeController(mockIncomeService, scopeService, mockAuthConnector, cc)
    val sandboxIncomeController =
      new SandboxIncomeController(new SandboxIncomeService, scopeService, mockAuthConnector, cc)
    given(scopeService.getEndPointScopes(any())).willReturn(Seq("hello-world"))
  }

  def externalServices: Seq[String] = Seq("Stub")

  "Income controller income function" should {
    val fakeRequest =
      FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString&toDate=$toDateString")

    "return 500 when matching succeeds and service returns payments" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
      //  .willReturn(successful(payments))

      val result = intercept[Exception] { await(liveIncomeController.income(matchId, interval)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 when matching succeeds and service returns no payments" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
      //  .willReturn(successful(Seq.empty))

      val result = intercept[Exception] { await(liveIncomeController.income(matchId, interval)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 with correct self link response when toDate is not provided in the request" in new Setup {

      val fakeRequest = FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString")

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
      //  .willReturn(successful(payments))

      val result = intercept[Exception] { await(liveIncomeController.income(matchId, interval)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "return 500 for an invalid matchId" in new Setup {

      // TODO reinstate when the V2 Income Service is coded up
      //given(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
      //  .willReturn(failed(new MatchNotFoundException()))

      val result = intercept[Exception] { await(liveIncomeController.income(matchId, interval)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      val result = intercept[Exception] { await(sandboxIncomeController.income(matchId, interval)(fakeRequest)) }
      assert(result.getMessage == "NOT_IMPLEMENTED")
    }

  }
}
