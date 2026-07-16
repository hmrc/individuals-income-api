package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
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
