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

package unit.uk.gov.hmrc.individualsincomeapi.audit

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.audit.v2.models._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{IncomePayeHelpers, IncomeSaHelpers, UnitSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class AuditHelperSpec extends UnitSpec with MockitoSugar with IncomePayeHelpers with IncomeSaHelpers {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val applicationId = "80a6bb14-d888-436e-a541-4000674c60bb"
  val request = FakeRequest().withHeaders("X-Application-Id" -> applicationId)
  val ifApiResponse = Seq(createValidPayeEntry())
  val apiResponse = Seq(Json.obj("paye" -> "test"))
  val ifSaApiResponse = Seq(createValidSaTaxYearEntry())
  val apiSaResponse = Json.obj("sa" -> "test")
  val ifUrl =
    s"host/individuals/income/paye/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val ifSaUrl =
    s"host/individuals/income/paye/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
  val endpoint = "/test"

  val auditConnector = mock[AuditConnector]

  val auditHelper = new AuditHelper(auditConnector)

  "Auth helper" should {

    "auditAuthScopes" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ScopesAuditEventModel])

      auditHelper.auditAuthScopes(matchId, scopes, request)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("AuthScopesAuditEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ScopesAuditEventModel]
      capturedEvent.apiVersion shouldEqual "2.0"
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.scopes shouldBe scopes
      capturedEvent.asInstanceOf[ScopesAuditEventModel].applicationId shouldBe applicationId

    }

    "auditApiResponse PAYE" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ApiPayeResponseEventModel])

      auditHelper.auditApiResponse(correlationId, matchId, scopes, request, endpoint, Some(apiResponse))

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ApiPayeResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.scopes shouldBe scopes
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.response shouldBe Some(apiResponse)

    }

    "auditApiResponse SA" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ApiSaResponseEventModel])

      auditHelper.auditSaApiResponse(correlationId, matchId, scopes, request, endpoint, Some(apiSaResponse))

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ApiSaResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.scopes shouldBe scopes
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.returnLinks shouldBe endpoint
      capturedEvent.response shouldBe Some(apiSaResponse)

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiFailureEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldEqual endpoint
      capturedEvent.response shouldEqual msg
    }

    "auditIfApiResponse PAYE" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfPayeApiResponseEventModel])

      auditHelper.auditIfPayeApiResponse(correlationId, matchId, request, ifUrl, ifApiResponse)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IntegrationFrameworkApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[IfPayeApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual correlationId
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldBe ifUrl
      capturedEvent.integrationFrameworkPaye shouldBe ifApiResponse

    }

    "auditIfApiResponse SA" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfSaApiResponseEventModel])

      auditHelper.auditIfSaApiResponse(correlationId, matchId, request, ifSaUrl, ifSaApiResponse)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IntegrationFrameworkApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[IfSaApiResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual correlationId
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldBe ifUrl
      capturedEvent.integrationFrameworkSa shouldBe ifSaApiResponse

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditIfApiFailure(correlationId, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IntegrationFrameworkApiFailureEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue.asInstanceOf[ApiFailureResponseEventModel]
      capturedEvent.matchId shouldEqual matchId
      capturedEvent.correlationId shouldEqual Some(correlationId)
      capturedEvent.applicationId shouldBe applicationId
      capturedEvent.requestUrl shouldEqual ifUrl
      capturedEvent.response shouldEqual msg

    }

  }

}
