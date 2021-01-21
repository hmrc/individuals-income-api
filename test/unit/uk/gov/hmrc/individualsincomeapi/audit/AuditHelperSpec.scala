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
import uk.gov.hmrc.individualsincomeapi.audit.v2.{AuditHelper, DefaultHttpExtendedAuditEvent, HttpExtendedAuditEvent}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.UnitSpec
import org.mockito.Mockito.{times, verify}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global

class AuditHelperSpec extends UnitSpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  val endpoint = "testAuditApiResponse"
  val correlationId = "test"
  val scopes = Some("test")
  val matchId = Some(UUID.fromString("80a6bb14-d888-436e-a541-4000674c60aa"))
  val request = FakeRequest()
  val response = Json.toJson("some" -> "json")

  val auditConnector = mock[AuditConnector]
  val httpExtendedAuditEvent = new DefaultHttpExtendedAuditEvent("individuals-income-api")

  val auditHelper = AuditHelper(auditConnector, httpExtendedAuditEvent)

  val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

  "Auth helper" should {

    "auditApiResponse" in {

      auditHelper.auditApiResponse(endpoint, correlationId, scopes, matchId, request, response)

      verify(auditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

      val result = Json.parse("""
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
      capturedEvent.asInstanceOf[ExtendedDataEvent].auditType shouldEqual "GET-testAuditApiResponse"
      capturedEvent.asInstanceOf[ExtendedDataEvent].detail shouldEqual result

    }

    "auditIfApiResponse" in {}

  }

}
