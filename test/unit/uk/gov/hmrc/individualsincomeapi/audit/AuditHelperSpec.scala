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

import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{IncomePayeHelpers, IncomeSaHelpers, UnitSpec}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.audit.v2.models.{ApiFailureResponseEventModel, ApiPayeResponseEventModel, ApiSaResponseEventModel, IfPayeApiResponseEventModel, IfSaApiResponseEventModel, ScopesAuditEventModel}

import scala.concurrent.ExecutionContext.Implicits.global

class AuditHelperSpec extends UnitSpec with MockitoSugar with IncomePayeHelpers with IncomeSaHelpers {

  implicit val hc = HeaderCarrier()

  val nino = "CS700100A"
  val correlationId = "test"
  val scopes = "test"
  val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
  val request = FakeRequest()
  val ifApiResponse = Seq(createValidPayeEntry())
  val apiResponse = Seq(Json.obj("paye" -> "test"))
  val ifSaApiResponse = Seq(createValidSaTaxYearEntry())
  val apiSaResponse = Json.obj("sa" -> "test")
  val ifUrl =
    s"host/individuals/employments/paye/nino/$nino?startDate=2019-01-01&endDate=2020-01-01&fields=some(vals(val1),val2)"
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

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ScopesAuditEventModel].apiVersion shouldEqual "2.0"
      capturedEvent.asInstanceOf[ScopesAuditEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ScopesAuditEventModel].scopes shouldBe scopes

    }

    "auditApiResponse PAYE" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ApiPayeResponseEventModel])

      auditHelper.auditApiResponse(correlationId, matchId, scopes, request, endpoint, Some(apiResponse))

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ApiPayeResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ApiPayeResponseEventModel].correlationId shouldEqual Some(correlationId)
      capturedEvent.asInstanceOf[ApiPayeResponseEventModel].scopes shouldBe scopes
      capturedEvent.asInstanceOf[ApiPayeResponseEventModel].returnLinks shouldBe endpoint
      capturedEvent.asInstanceOf[ApiPayeResponseEventModel].response shouldBe Some(apiResponse)

    }

    "auditApiResponse SA" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[ApiSaResponseEventModel])

      auditHelper.auditSaApiResponse(correlationId, matchId, scopes, request, endpoint, Some(apiSaResponse))

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ApiSaResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ApiSaResponseEventModel].correlationId shouldEqual Some(correlationId)
      capturedEvent.asInstanceOf[ApiSaResponseEventModel].scopes shouldBe scopes
      capturedEvent.asInstanceOf[ApiSaResponseEventModel].returnLinks shouldBe endpoint
      capturedEvent.asInstanceOf[ApiSaResponseEventModel].response shouldBe Some(apiSaResponse)

    }

    "auditApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditApiFailure(Some(correlationId), matchId, request, "/test", msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("ApiFailureEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].correlationId shouldEqual Some(correlationId)
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].requestUrl shouldEqual endpoint
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].response shouldEqual msg
    }

    "auditIfApiResponse PAYE" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfPayeApiResponseEventModel])

      auditHelper.auditIfPayeApiResponse(correlationId, matchId, request, ifUrl, ifApiResponse)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IfApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[IfPayeApiResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[IfPayeApiResponseEventModel].correlationId shouldEqual correlationId
      capturedEvent.asInstanceOf[IfPayeApiResponseEventModel].requestUrl shouldBe ifUrl
      capturedEvent.asInstanceOf[IfPayeApiResponseEventModel].ifPaye shouldBe ifApiResponse

    }

    "auditIfApiResponse SA" in {

      Mockito.reset(auditConnector)

      val captor = ArgumentCaptor.forClass(classOf[IfSaApiResponseEventModel])

      auditHelper.auditIfSaApiResponse(correlationId, matchId, request, ifUrl, ifSaApiResponse)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IfApiResponseEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[IfSaApiResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[IfSaApiResponseEventModel].correlationId shouldEqual correlationId
      capturedEvent.asInstanceOf[IfSaApiResponseEventModel].requestUrl shouldBe ifUrl
      capturedEvent.asInstanceOf[IfSaApiResponseEventModel].ifSa shouldBe ifSaApiResponse

    }

    "auditIfApiFailure" in {

      Mockito.reset(auditConnector)

      val msg = "Something went wrong"

      val captor = ArgumentCaptor.forClass(classOf[ApiFailureResponseEventModel])

      auditHelper.auditIfApiFailure(correlationId, matchId, request, ifUrl, msg)

      verify(auditConnector, times(1)).sendExplicitAudit(eqTo("IfApiFailureEvent"),
        captor.capture())(any(), any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].matchId shouldEqual matchId
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].correlationId shouldEqual Some(correlationId)
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].requestUrl shouldEqual ifUrl
      capturedEvent.asInstanceOf[ApiFailureResponseEventModel].response shouldEqual msg

    }

  }

}
