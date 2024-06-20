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

package component.uk.gov.hmrc.individualsincomeapi.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json

object IndividualsMatchingApiStub extends MockHost(21000) {

  def willRespondWith(matchId: String, responseCode: Int, responseBody: String = "") =
    mock.register(
      get(urlEqualTo(s"/match-record/$matchId"))
        .willReturn(aResponse().withStatus(responseCode).withBody(responseBody))
    )

  def hasMatchFor(matchId: String, nino: String): Unit =
    mock.register(
      get(s"/match-record/$matchId")
        .willReturn(
          okJson(
            Json
              .obj(
                "matchId" -> matchId,
                "nino"    -> nino
              )
              .toString
          )
        )
    )

}
