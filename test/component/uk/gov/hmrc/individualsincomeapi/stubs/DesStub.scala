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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.DesEmployments
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters._

object DesStub extends MockHost(23000) {

  def searchEmploymentIncomeForPeriodReturns(nino: String, fromDate: String, toDate: String, desEmployments: DesEmployments) = {
    mock.register(get(urlPathEqualTo(s"/individuals/nino/$nino/employments/income"))
      .withQueryParam("from", equalTo(fromDate))
      .withQueryParam("to", equalTo(toDate))
      .willReturn(aResponse().withStatus(Status.OK).withBody(Json.toJson(desEmployments).toString())))
  }

  def searchEmploymentIncomeReturnsNoIncomeFor(nino: String, fromDate: String, toDate: String) = {
    mock.register(get(urlPathEqualTo(s"/individuals/nino/$nino/employments/income"))
      .withQueryParam("from", equalTo(fromDate))
      .withQueryParam("to", equalTo(toDate))
      .willReturn(aResponse().withStatus(Status.NOT_FOUND)))
  }
}