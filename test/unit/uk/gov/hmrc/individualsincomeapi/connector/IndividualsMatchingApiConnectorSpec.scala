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

package unit.uk.gov.hmrc.individualsincomeapi.connector

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.individualsincomeapi.connector.IndividualsMatchingApiConnector
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import utils.SpecBase

import java.util.UUID
import scala.concurrent.ExecutionContext

class IndividualsMatchingApiConnectorSpec extends SpecBase with Matchers with BeforeAndAfterEach {

  val stubPort = sys.env.getOrElse("WIREMOCK", "11121").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  implicit val ec: ExecutionContext = ExecutionContext.global

  trait Fixture {
    implicit val hc = HeaderCarrier()

    val individualsMatchingApiConnector =
      new IndividualsMatchingApiConnector(servicesConfig, fakeApplication().injector.instanceOf[HttpClient]) {
        override val serviceUrl = "http://127.0.0.1:11121"
      }
  }

  def externalServices: Seq[String] = Seq("Stub")

  override def beforeEach(): Unit = {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  "Matching API connector resolve function should" should {

    val matchId = UUID.randomUUID()

    def stubWithResponseStatus(responseStatus: Int, body: String = ""): Unit =
      stubFor(
        get(urlPathMatching(s"/match-record/$matchId"))
          .willReturn(aResponse().withStatus(responseStatus).withBody(body)))

    "fail when upstream service fails" in new Fixture {
      stubWithResponseStatus(INTERNAL_SERVER_ERROR)
      a[UpstreamErrorResponse] should be thrownBy {
        await(individualsMatchingApiConnector.resolve(matchId))
      }
    }

    "rethrow a not found exception as a match not found exception" in new Fixture {
      stubWithResponseStatus(NOT_FOUND)
      a[MatchNotFoundException] should be thrownBy {
        await(individualsMatchingApiConnector.resolve(matchId))
      }
    }

    "return a nino match when upstream service call succeeds" in new Fixture {
      stubWithResponseStatus(
        OK,
        s"""
          {
            "matchId":"${matchId.toString}",
            "nino":"AB123456C"
          }
        """)
      await(individualsMatchingApiConnector.resolve(matchId)) shouldBe MatchedCitizen(matchId, Nino("AB123456C"))
    }

  }

  override def afterEach(): Unit =
    wireMockServer.stop()

}
