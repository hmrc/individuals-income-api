/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsincomeapi.play

import java.util.UUID

import org.scalatest.Matchers
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, GET}
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils._
import utils.TestSupport

class RequestHeaderUtilsSpec extends TestSupport with Matchers {

  "getVersionedUri" should {
    "return the versioned request when the Accept header is set" in {
      val fooRequest = FakeRequest(GET, "/foo")

      getVersionedRequest(fooRequest.withHeaders(ACCEPT -> "application/vnd.hmrc.2.0+json")).uri shouldBe "/v2.0/foo"
      getVersionedRequest(fooRequest.withHeaders(ACCEPT -> "application/vnd.hmrc.2.0+json")).path shouldBe "/v2.0/foo"
    }

    "return the versioned request for the root endpoint when the Accept header is set" in {
      val fooRequest = FakeRequest(GET, "/")

      getVersionedRequest(fooRequest.withHeaders(ACCEPT -> "application/vnd.hmrc.2.0+json")).uri shouldBe "/v2.0"
      getVersionedRequest(fooRequest.withHeaders(ACCEPT -> "application/vnd.hmrc.2.0+json")).path shouldBe "/v2.0"
    }

    "Default to v1.0 when the Accept header is not set" in {
      val fooRequest = FakeRequest(GET, "/foo")

      getVersionedRequest(fooRequest).uri shouldBe "/v1.0/foo"
      getVersionedRequest(fooRequest).path shouldBe "/v1.0/foo"
    }
  }

  "extractUriContext" should {
    "extract uri contexts" in {
      extractUriContext(FakeRequest(GET, "/")) shouldBe "/"
      extractUriContext(FakeRequest(GET, "/foo")) shouldBe "/foo"
      extractUriContext(FakeRequest(GET, "/foo/bar")) shouldBe "/foo"
    }
  }

  "getClientIdHeader" should {
    "extract the client id header from the request if present" in {
      val clientId = UUID.randomUUID().toString
      val fooRequest = FakeRequest(GET, "/")

      getClientIdHeader(fooRequest.withHeaders(CLIENT_ID_HEADER -> clientId)) shouldBe (CLIENT_ID_HEADER -> clientId)
    }

    "generate a new header with the default value dash (-) if the header is not present" in {
      getClientIdHeader(FakeRequest(GET, "/")) shouldBe (CLIENT_ID_HEADER -> "-")
    }
  }
}
