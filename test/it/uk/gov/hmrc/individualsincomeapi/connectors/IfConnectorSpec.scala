/*
 * Copyright 2022 HM Revenue & Customs
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

package it.uk.gov.hmrc.individualsincomeapi.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, InternalServerException, NotFoundException}
import uk.gov.hmrc.individualsincomeapi.connector.IfConnector
import uk.gov.hmrc.integration.ServiceSpec
import unit.uk.gov.hmrc.individualsincomeapi.util._
import utils._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPaye, IfSa}
import uk.gov.hmrc.individualsincomeapi.domain.{TaxYear, TaxYearInterval}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IfConnectorSpec
    extends AnyWordSpec with Matchers with BeforeAndAfterEach with ServiceSpec with MockitoSugar with TestDates
    with TestSupport with IncomePayeHelpers with IncomeSaHelpers {

  val stubPort = sys.env.getOrElse("WIREMOCK", "11122").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val integrationFrameworkAuthorizationToken = "IF_TOKEN"
  val integrationFrameworkEnvironment = "IF_ENVIRONMENT"
  val clientId = "CLIENT_ID"

  override def fakeApplication() = new GuiceApplicationBuilder()
    .bindings(bindModules: _*)
    .configure(
      "microservice.services.integration-framework.host"         -> "127.0.0.1",
      "microservice.services.integration-framework.port"                -> "11122",
      "microservice.services.integration-framework.authorization-token" -> integrationFrameworkAuthorizationToken,
      "microservice.services.integration-framework.environment"         -> integrationFrameworkEnvironment
    )
    .build()

  trait Setup {

    val matchId = "80a6bb14-d888-436e-a541-4000674c60aa"
    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader = ("CorrelationId" -> sampleCorrelationId)

    implicit val hc = HeaderCarrier()

    val config = app.injector.instanceOf[ServicesConfig]
    val httpClient = app.injector.instanceOf[HttpClient]
    val auditHelper = mock[AuditHelper]
    val underTest = new IfConnector(config, httpClient, auditHelper)

    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  }

  def externalServices: Seq[String] = Seq.empty

  override def beforeEach() {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  override def afterEach() {
    wireMockServer.stop()
  }

  val incomePayeNoData = IfPaye(Seq())
  val incomePayeSingle = IfPaye(Seq(createValidPayeEntry()))
  val incomePayeMulti = IfPaye(Seq(createValidPayeEntry(), createValidPayeEntry()))
  val incomeSaNoData = IfSa(Seq())
  val incomeSaSingle = IfSa(Seq(createValidSaTaxYearEntry()))
  val incomeSaMulti = IfSa(Seq(createValidSaTaxYearEntry(), createValidSaTaxYearEntry()))

  "fetchPaye" should {

    val nino = Nino("NA000799C")
    val startDate = "2016-01-01"
    val endDate = "2017-03-01"
    val interval = toInterval(startDate, endDate)
    val fields = "some(fields(one, two, three))"

    "for no paye data" should {

      "return en empty sequence" in new Setup {

        Mockito.reset(underTest.auditHelper)

        stubFor(
          get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
            .withQueryParam("startDate", equalTo(startDate))
            .withQueryParam("endDate", equalTo(endDate))
            .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .withHeader("CorrelationId", equalTo(sampleCorrelationId))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.toJson(incomePayeNoData).toString())))

        val result = await(
          underTest
            .fetchPayeIncome(nino, interval, Some(fields), matchId)(
              hc,
              FakeRequest().withHeaders(sampleCorrelationIdHeader),
              ec
            )
        )

        verify(underTest.auditHelper, times(1))
          .auditIfPayeApiResponse(any(), any(), any(), any(), any())(any())

        result shouldBe incomePayeNoData.paye

      }
    }

    "for single paye data found" should {

      "return single paye data" in new Setup {

        Mockito.reset(underTest.auditHelper)

        stubFor(
          get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
            .withQueryParam("startDate", equalTo(startDate))
            .withQueryParam("endDate", equalTo(endDate))
            .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .withHeader("CorrelationId", equalTo(sampleCorrelationId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(
                  Json.toJson(incomePayeSingle).toString()
                )
            )
        )

        val result = await(
          underTest
            .fetchPayeIncome(nino, interval, Some(fields), matchId)(
              hc,
              FakeRequest().withHeaders(sampleCorrelationIdHeader),
              ec))

        verify(underTest.auditHelper, times(1))
          .auditIfPayeApiResponse(any(), any(), any(), any(), any())(any())

        result shouldBe incomePayeSingle.paye

      }
    }

    "for multi paye data found" should {

      "return multi paye data" in new Setup {

        Mockito.reset(underTest.auditHelper)

        stubFor(
          get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
            .withQueryParam("startDate", equalTo(startDate))
            .withQueryParam("endDate", equalTo(endDate))
            .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .withHeader("CorrelationId", equalTo(sampleCorrelationId))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.toJson(incomePayeMulti).toString())))

        val result = await(
          underTest
            .fetchPayeIncome(nino, interval, Some(fields), matchId)(
              hc,
              FakeRequest().withHeaders(sampleCorrelationIdHeader),
              ec))

        verify(underTest.auditHelper, times(1))
          .auditIfPayeApiResponse(any(), any(), any(), any(), any())(any())

        result shouldBe incomePayeMulti.paye

      }
    }

    "fail when IF returns an error" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
          .willReturn(aResponse().withStatus(500)))

      intercept[InternalServerException] {
        await(underTest
          .fetchPayeIncome(nino, interval, None, matchId)(hc, FakeRequest().withHeaders(sampleCorrelationIdHeader), ec))
      }

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(any())

    }

    "fail when IF returns a bad request" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
          .willReturn(aResponse().withStatus(400)))

      intercept[InternalServerException] {
        await(underTest
          .fetchPayeIncome(nino, interval, None, matchId)(hc, FakeRequest().withHeaders(sampleCorrelationIdHeader), ec))
      }

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(any())

    }

    "return an empty dataset for NO_DATA_FOUND" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
          .willReturn(aResponse().withStatus(404).withBody("NO_DATA_FOUND")))

      val result = await(underTest.fetchPayeIncome(nino, interval, None, matchId)
      (hc, FakeRequest().withHeaders(sampleCorrelationIdHeader), ec))

      result shouldBe List()

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(any())

    }

    "fail when IF returns a NOT_FOUND" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
          .willReturn(aResponse().withStatus(404).withBody("NOT_FOUND")))

      intercept[NotFoundException] {
        await(underTest
          .fetchPayeIncome(nino, interval, None, matchId)(hc, FakeRequest().withHeaders(sampleCorrelationIdHeader), ec))
      }

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(any())

    }
  }

  "fetchSa" should {

    val nino = Nino("NA000799C")
    val startYear = "2016"
    val endYear = "2017"
    val interval = TaxYearInterval(TaxYear("2015-16"), TaxYear("2016-17"))
    val fields = "some(fields(one, two, three))"

    "for no self assessment data" should {

      "return en empty sequence" in new Setup {

        Mockito.reset(underTest.auditHelper)

        stubFor(
          get(urlPathMatching(s"/individuals/income/sa/nino/$nino"))
            .withQueryParam("startYear", equalTo(startYear))
            .withQueryParam("endYear", equalTo(endYear))
            .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .withHeader("CorrelationId", equalTo(sampleCorrelationId))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.toJson(incomeSaNoData).toString())))

        val result = await {
          underTest.fetchSelfAssessmentIncome(nino, interval, Some(fields), matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        }

        result shouldBe incomeSaNoData.sa

        verify(underTest.auditHelper, times(1))
          .auditIfSaApiResponse(any(), any(), any(), any(), any())(any())

      }
    }

    "for single self assessment data found" should {

      "return single self assessment data" in new Setup {

        Mockito.reset(underTest.auditHelper)

        stubFor(
          get(urlPathMatching(s"/individuals/income/sa/nino/$nino"))
            .withQueryParam("startYear", equalTo(startYear))
            .withQueryParam("endYear", equalTo(endYear))
            .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .withHeader("CorrelationId", equalTo(sampleCorrelationId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(
                  Json.toJson(incomeSaSingle).toString()
                )
            )
        )

        val result = await {
          underTest.fetchSelfAssessmentIncome(nino, interval, Some(fields), matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        }

        result shouldBe incomeSaSingle.sa

        verify(underTest.auditHelper, times(1))
          .auditIfSaApiResponse(any(), any(), any(), any(), any())(any())

      }
    }

    "for multi self assessment data found" should {

      "return multi self assessment data" in new Setup {

        Mockito.reset(underTest.auditHelper)

        stubFor(
          get(urlPathMatching(s"/individuals/income/sa/nino/$nino"))
            .withQueryParam("startYear", equalTo(startYear))
            .withQueryParam("endYear", equalTo(endYear))
            .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .withHeader("CorrelationId", equalTo(sampleCorrelationId))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.toJson(incomeSaMulti).toString())))

        val result = await {
          underTest.fetchSelfAssessmentIncome(nino, interval, Some(fields), matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        }

        result shouldBe incomeSaMulti.sa

        verify(underTest.auditHelper, times(1))
          .auditIfSaApiResponse(any(), any(), any(), any(), any())(any())

      }
    }

    "fail when IF returns an error" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/income/sa/nino/$nino"))
          .willReturn(aResponse().withStatus(500)))

      intercept[InternalServerException] {
        await {
          underTest.fetchSelfAssessmentIncome(nino, interval, None, matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        }
      }

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(any())

    }

    "fail when IF returns a bad request" in new Setup {

      Mockito.reset(underTest.auditHelper)

      stubFor(
        get(urlPathMatching(s"/individuals/income/sa/nino/$nino"))
          .willReturn(aResponse().withStatus(400)))

      intercept[InternalServerException] {
        await {
          underTest.fetchSelfAssessmentIncome(nino, interval, None, matchId)(
            hc,
            FakeRequest().withHeaders(sampleCorrelationIdHeader),
            ec
          )
        }
      }

      verify(underTest.auditHelper, times(1))
        .auditIfApiFailure(any(), any(), any(), any(), any())(any())

    }
  }
}
