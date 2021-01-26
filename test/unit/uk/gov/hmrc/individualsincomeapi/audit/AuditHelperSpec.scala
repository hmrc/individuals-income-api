/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsincomeapi.audit

import java.util.UUID

import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.{AuditHelper, DefaultHttpExtendedAuditEvent}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.UnitSpec
import org.mockito.Mockito.{times, verify}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, Mockito}
import play.api.test.FakeRequest
import uk.gov.hmrc.individualsincomeapi.audit.v2.models.{ApiAuditRequest, ApiIfAuditRequest, ApiIfFailureAuditRequest}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global

class AuditHelperSpec extends UnitSpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = Some("test")
  val matchId = Some("80a6bb14-d888-436e-a541-4000674c60aa")
  val request = FakeRequest()
  val response = Json.toJson("some" -> "json")
  val ifUrl =
    s"host/individuals/income/paye/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"

  val auditConnector = mock[AuditConnector]
  val httpExtendedAuditEvent = new DefaultHttpExtendedAuditEvent("individuals-income-api")

  val auditHelper = AuditHelper(auditConnector, httpExtendedAuditEvent)

  "Auth helper" should {

    "auditApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      val req = ApiAuditRequest(correlationId, scopes, matchId, request, response)

      auditHelper.auditApiResponse(req)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "2.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "response": "[\"some\",\"json\"]",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "token": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-income-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "ApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditIfApiResponse" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      val req = ApiIfAuditRequest(correlationId, scopes, matchId, request, ifUrl, response)

      auditHelper.auditIfApiResponse(req)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "2.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl": "host/individuals/income/paye/nino/CS700100A?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)",
          |  "response": "[\"some\",\"json\"]",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "token": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-income-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "IfApiResponseEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      val req = ApiIfFailureAuditRequest(correlationId, scopes, matchId, request, ifUrl)

      auditHelper.auditIfApiFailure(req, msg)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse(
        """
          |{
          |  "apiVersion": "2.0",
          |  "matchId": "80a6bb14-d888-436e-a541-4000674c60aa",
          |  "correlationId": "test",
          |  "scopes": "test",
          |  "requestUrl": "host/individuals/income/paye/nino/CS700100A?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)",
          |  "response": "Something went wrong",
          |  "method": "GET",
          |  "deviceID": "-",
          |  "ipAddress": "-",
          |  "token": "-",
          |  "referrer": "-",
          |  "Authorization": "-",
          |  "input": "Request to /",
          |  "userAgentString": "-"
          |}
          |""".stripMargin)

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditSource shouldEqual "individuals-income-api"
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "IfApiFailureEvent"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldBe result

    }

  }

}
