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
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{times, verify, verifyNoInteractions}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, TooManyRequestException}
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v1.{LiveIncomeController, SandboxIncomeController}
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v1.Payment
import uk.gov.hmrc.individualsincomeapi.domain.v1.Payment.paymentJsonFormat
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxMatchId
import uk.gov.hmrc.individualsincomeapi.services.v1.{LiveIncomeService, SandboxIncomeService}
import uk.gov.hmrc.individualsincomeapi.util.Interval
import utils.SpecBase

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.{failed, successful}

class IncomeControllerSpec extends SpecBase with MockitoSugar {
  implicit lazy val materializer: Materializer = fakeApplication().materializer

  val matchId: UUID = UUID.randomUUID()
  val fromDateString = "2017-03-02"
  val toDateString = "2017-05-31"
  val interval: Interval =
    Interval(LocalDate.parse(fromDateString).atStartOfDay(), LocalDate.parse(toDateString).atStartOfDay())
  val payments: Seq[Payment] = Seq(
    Payment(1000.50, LocalDate.parse("2016-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10))
  )
  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val mockIncomeService: LiveIncomeService = mock[LiveIncomeService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockAuditHelper: AuditHelper = mock[AuditHelper]

    val liveIncomeController = new LiveIncomeController(mockIncomeService, mockAuthConnector, cc, mockAuditHelper)
    val sandboxIncomeController =
      new SandboxIncomeController(new SandboxIncomeService, mockAuthConnector, cc, mockAuditHelper)

    `given`(mockAuthConnector.authorise(any(), refEq(EmptyRetrieval))(any(), any())).willReturn(successful(()))
  }

  def externalServices: Seq[String] = Seq("Stub")

  "Income controller income function" should {
    val fakeRequest =
      FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString&toDate=$toDateString")

    "return 200 (OK) when matching succeeds and service returns payments" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(successful(payments))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(fakeRequest.uri))
    }

    "return 200 (OK) when matching succeeds and service returns no payments" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(successful(Seq.empty))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(fakeRequest.uri, Seq.empty))
    }

    "return 200 (OK) with correct self link response when toDate is not provided in the request" in new Setup {
      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString")

      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(successful(payments))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.parse(expectedPayload(fakeRequest.uri))
    }

    "return 404 (Not Found) for an invalid matchId" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"NOT_FOUND", "message":"The resource can not be found"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return 401 (UNAUTHORIZED) for an invalid matchId" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(failed(InsufficientEnrolments()))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"UNAUTHORIZED", "message":"Insufficient Enrolments"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return 429 (TOO_MANY_REQUESTS) for an invalid matchId" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(failed(new TooManyRequestException("Too Many Requests")))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe TOO_MANY_REQUESTS
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"TOO_MANY_REQUESTS", "message":"Rate limit exceeded"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return 400 (BAD_REQUEST) for an invalid matchId" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(failed(new BadRequestException("Bad Request")))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"INVALID_REQUEST", "message":"Bad Request"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return 400 (Illegal Argument) for an invalid matchId" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(failed(new IllegalArgumentException("Illegal Argument")))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"INVALID_REQUEST", "message":"Illegal Argument"}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return 500 (Internal Server) for an invalid matchId" in new Setup {
      `given`(mockIncomeService.fetchIncomeByMatchId(refEq(matchId), refEq(interval))(any()))
        .willReturn(failed(new Exception()))

      val result: Result = await(liveIncomeController.income(matchId, interval)(fakeRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.parse(s"""{"code":"INTERNAL_SERVER_ERROR", "message":"Something went wrong."}""")
      verify(mockAuditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "not require bearer token authentication for Sandbox" in new Setup {
      val result: Result = await(sandboxIncomeController.income(sandboxMatchId, interval)(fakeRequest))

      status(result) shouldBe OK
      verifyNoInteractions(mockAuthConnector)
    }

  }

  private def expectedPayload(uri: String, payments: Seq[Payment] = payments) =
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
