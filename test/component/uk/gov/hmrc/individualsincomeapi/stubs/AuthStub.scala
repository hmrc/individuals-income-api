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

package component.uk.gov.hmrc.individualsincomeapi.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsArray
import play.api.libs.json.Json._
import uk.gov.hmrc.auth.core.Enrolment

object AuthStub extends MockHost(22000) {

  private def privilegedAuthority(scope: String) = obj(
    "authorise" -> arr(toJson(Enrolment(scope))),
    "retrieve" -> JsArray()
  )

  def willAuthorizePrivilegedAuthToken(authBearerToken: String, scope: String): StubMapping = {
    mock.register(post(urlEqualTo("/auth/authorise"))
      .withRequestBody(equalToJson(privilegedAuthority(scope).toString()))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody("""{"internalId": "some-id"}""")))
  }

  def willNotAuthorizePrivilegedAuthToken(authBearerToken: String, scope: String): StubMapping = {
    mock.register(post(urlEqualTo("/auth/authorise"))
      .withRequestBody(equalToJson(privilegedAuthority(scope).toString()))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse()
        .withStatus(Status.UNAUTHORIZED)
        .withHeader(HeaderNames.WWW_AUTHENTICATE, """MDTP detail="Bearer token is missing or not authorized"""")))
  }

  def willAuthorizeNinoWithAuthToken(nino: String, authBearerToken: String) = {
    mock.register(get(urlEqualTo(s"/authorise/read/paye/$nino?confidenceLevel=50"))
      .withHeader(AUTHORIZATION, equalTo(authBearerToken))
      .willReturn(aResponse().withStatus(Status.OK)))
  }
}