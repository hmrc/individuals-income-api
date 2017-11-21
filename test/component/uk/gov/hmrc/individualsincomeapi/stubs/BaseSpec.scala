/*
 * Copyright 2017 HM Revenue & Customs
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

package component.uk.gov.hmrc.individualsincomeapi.stubs

import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.MimeTypes.JSON
import uk.gov.hmrc.individualsincomeapi.cache.ShortLivedCache

import scala.concurrent.Await.result
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

trait BaseSpec extends FeatureSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with GuiceOneServerPerSuite
  with GivenWhenThen {

  override lazy val port = 9000
  implicit override lazy val app: Application = GuiceApplicationBuilder().configure(
    "auditing.enabled" -> false,
    "auditing.traceRequests" -> false,
    "microservice.services.auth.port" -> AuthStub.port,
    "microservice.services.individuals-matching-api.port" -> IndividualsMatchingApiStub.port,
    "microservice.services.des.port" -> DesStub.port,
    "mongodb.uri" -> "mongodb://localhost:27017/individuals-income-api-it",
    "run.mode" -> "It"
  ).build()

  val timeout = Duration(5, TimeUnit.SECONDS)
  val serviceUrl = s"http://localhost:$port"
  val mocks = Seq(AuthStub, IndividualsMatchingApiStub, DesStub)
  val shortLivedCache = app.injector.instanceOf[ShortLivedCache]
  val authToken = "Bearer AUTH_TOKEN"
  val acceptHeaderV1 = ACCEPT -> "application/vnd.hmrc.1.0+json"
  val acceptHeaderP1 = ACCEPT -> "application/vnd.hmrc.P1.0+json"

  protected def requestHeaders(acceptHeader: (String, String) = acceptHeaderV1) = {
    Map(CONTENT_TYPE -> JSON, AUTHORIZATION -> authToken, acceptHeader)
  }

  protected def errorResponse(message: String) = {
    s"""{"code":"INVALID_REQUEST","message":"$message"}"""
  }

  override protected def beforeEach(): Unit = {
    mocks.foreach(m => if (!m.server.isRunning) m.server.start())
    result(shortLivedCache.cacheRepository.repo.drop, timeout)
  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.mock.resetMappings())
  }

  override def afterAll(): Unit = {
    mocks.foreach(_.server.stop())
    result(shortLivedCache.cacheRepository.repo.drop, timeout)
  }
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
  val url = s"http://localhost:$port"
}
