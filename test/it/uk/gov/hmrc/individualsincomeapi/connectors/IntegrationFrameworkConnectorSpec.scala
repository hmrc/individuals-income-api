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

package it.uk.gov.hmrc.individualsincomeapi.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.connector.IntegrationFrameworkConnector
import uk.gov.hmrc.integration.ServiceSpec
import unit.uk.gov.hmrc.individualsincomeapi.util._
import utils._
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IncomePaye

import scala.concurrent.ExecutionContext.Implicits.global

class IntegrationFrameworkConnectorSpec
    extends WordSpec with Matchers with BeforeAndAfterEach with ServiceSpec with MockitoSugar with TestDates
    with TestSupport with IncomePayeHelpers {

  val stubPort = sys.env.getOrElse("WIREMOCK", "11122").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val integrationFrameworkAuthorizationToken = "IF_TOKEN"
  val integrationFrameworkEnvironment = "IF_ENVIRONMENT"
  val clientId = "CLIENT_ID"

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .bindings(bindModules: _*)
    .configure(
      "microservice.services.integration-framework.host"                -> "localhost",
      "microservice.services.integration-framework.port"                -> "11122",
      "microservice.services.integration-framework.authorization-token" -> integrationFrameworkAuthorizationToken,
      "microservice.services.integration-framework.environment"         -> integrationFrameworkEnvironment
    )
    .build()

  trait Setup {
    implicit val hc = HeaderCarrier()

    val underTest = fakeApplication.injector.instanceOf[IntegrationFrameworkConnector]
  }

  def externalServices: Seq[String] = Seq.empty

  override def beforeEach() {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  override def afterEach() {
    wireMockServer.stop()
  }

  val incomePayeNoData = IncomePaye(Seq())
  val incomePayeSingle = paye.IncomePaye(Seq(createValidPayeEntry()))
  val incomePayeMulti = paye.IncomePaye(Seq(createValidPayeEntry(), createValidPayeEntry()))

  "fetchPaye" should {
    val nino = Nino("NA000799C")
    val startDate = "2016-01-01"
    val endDate = "2017-03-01"
    val interval = toInterval(startDate, endDate)

    "for no paye data" should {

      "return en empty sequence" in new Setup {

        stubFor(
          get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
            .withQueryParam("startDate", equalTo(startDate))
            .withQueryParam("endDate", equalTo(endDate))
            .withHeader("Authorization", equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.toJson(incomePayeNoData).toString())))

        val result = await(underTest.fetchPayeIncome(nino, interval, None))
        result shouldBe incomePayeNoData.paye

      }
    }

    "for single paye data found" should {

      "return single paye data" in new Setup {

        stubFor(
          get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
            .withQueryParam("startDate", equalTo(startDate))
            .withQueryParam("endDate", equalTo(endDate))
            .withHeader("Authorization", equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(
                  Json.toJson(incomePayeSingle).toString()
                )
            )
        )

        val result = await(underTest.fetchPayeIncome(nino, interval, None))
        result shouldBe incomePayeSingle.paye

      }
    }

    "for multi paye data found" should {

      "return multi paye data" in new Setup {

        stubFor(
          get(urlPathMatching(s"/individuals/income/paye/nino/$nino"))
            .withQueryParam("startDate", equalTo(startDate))
            .withQueryParam("endDate", equalTo(endDate))
            .withHeader("Authorization", equalTo(s"Bearer $integrationFrameworkAuthorizationToken"))
            .withHeader("Environment", equalTo(integrationFrameworkEnvironment))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(Json.toJson(incomePayeMulti).toString())))

        val result = await(underTest.fetchPayeIncome(nino, interval, None))
        result shouldBe incomePayeMulti.paye

      }
    }
  }
}
