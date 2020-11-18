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
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.integration.ServiceSpec
import unit.uk.gov.hmrc.individualsincomeapi.util.TestDates
import utils.TestSupport

import scala.concurrent.ExecutionContext.Implicits.global

class IntegrationFrameworkConnectorSpec
    extends WordSpec with Matchers with BeforeAndAfterEach with ServiceSpec with MockitoSugar with TestDates
    with TestSupport {

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

  val ifPaye = Seq(Paye("one"), Paye("two"))
  val ifSa = Seq(Sa("one"), Sa("two"))

  "fetchPaye" should {
    val nino = Nino("NA000799C")
    val startDate = "2016-01-01"
    val endDate = "2017-03-01"
    val interval = toInterval(startDate, endDate)

    "return the paye datait:test" in new Setup {
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
                """
             {
               "paye": [
                 {"id":"one"},
                 {"id":"two"}
               ]
             }
          """
              )))

      val result = await(underTest.fetchPayeIncome(nino, interval, None))

      result shouldBe Seq(ifPaye)
    }
  }
}
