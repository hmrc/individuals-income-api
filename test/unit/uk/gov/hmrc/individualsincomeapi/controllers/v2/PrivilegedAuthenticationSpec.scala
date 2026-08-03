/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Environment, Mode}
import play.api.http.Status.*
import play.api.mvc.{RequestHeader, Results}
import play.api.test.*
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.config.AppConfig
import uk.gov.hmrc.individualsincomeapi.controllers.v2.PrivilegedAuthentication

import scala.concurrent.{ExecutionContext, Future}

class PrivilegedAuthenticationSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  object TestAuth extends PrivilegedAuthentication {
    override val authConnector: AuthConnector = mock[AuthConnector]
  }

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: RequestHeader = FakeRequest()
  implicit val auditHelper: AuditHelper = mock[AuditHelper]
  implicit val env: Environment = Environment.simple(mode = Mode.Dev)
  val appConfig: AppConfig = mock[AppConfig]
  when(appConfig.localEnv).thenReturn(true)
  "authenticate" should {

    "execute the supplied function in local mode" in {

      val expected = Results.Ok

      val result =
        TestAuth.authenticate(
          endpointScopes = List("scope1", "scope2"),
          matchId = "123"
        ) { scopes =>
          scopes should contain allOf ("scope1", "scope2")
          Future.successful(expected)
        }(using hc, request, auditHelper, ec, appConfig)

      status(result) shouldBe OK
    }
  }
}
