/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPaye, IfSa}

object IfStub extends MockHost(24000) {

  def searchPayeIncomeForPeriodReturns(nino: String, fromDate: String, toDate: String, fields: String, ifPaye: IfPaye) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/income/paye/nino/$nino"))
        .withQueryParam("startDate", equalTo(fromDate))
        .withQueryParam("endDate", equalTo(toDate))
        .withQueryParam("fields", equalTo(fields))
        .willReturn(aResponse().withStatus(Status.OK).withBody(Json.toJson(ifPaye).toString())))

  def searchSaIncomeForPeriodReturns(nino: String, fromTaxYear: String, toTaxYear: String, fields: String, ifSa: IfSa) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/income/sa/nino/$nino"))
        .withQueryParam("startYear", equalTo(fromTaxYear))
        .withQueryParam("endYear", equalTo(toTaxYear))
        .withQueryParam("fields", equalTo(fields))
        .willReturn(aResponse().withStatus(Status.OK).withBody(Json.toJson(ifSa).toString())))

  def searchPayeIncomeReturnsNoIncomeFor(nino: String, fromDate: String, toDate: String, fields: String) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/income/paye/nino/$nino"))
        .withQueryParam("startDate", equalTo(fromDate))
        .withQueryParam("endDate", equalTo(toDate))
        .withQueryParam("fields", equalTo(fields))
        .willReturn(aResponse().withStatus(Status.NOT_FOUND)))

  def searchSaIncomeReturnsNoIncomeFor(nino: String, fromTaxYear: String, toTaxYear: String, fields: String) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/income/sa/nino/$nino"))
        .withQueryParam("startYear", equalTo(fromTaxYear))
        .withQueryParam("endYear", equalTo(toTaxYear))
        .withQueryParam("fields", equalTo(fields))
        .willReturn(aResponse().withStatus(Status.NOT_FOUND)))

  def searchPayeIncomeReturnsRateLimitErrorFor(nino: String, fromDate: String, toDate: String, fields: String): Unit =
    mock.register(
      get(urlPathEqualTo(s"/individuals/income/paye/nino/$nino"))
        .withQueryParam("startDate", equalTo(fromDate))
        .withQueryParam("endDate", equalTo(toDate))
        .withQueryParam("fields", equalTo(fields))
        .willReturn(aResponse().withStatus(Status.TOO_MANY_REQUESTS)))

  def searchSaIncomeReturnsRateLimitErrorFor(
    nino: String,
    fromTaxYear: String,
    toTaxYear: String,
    fields: String): Unit =
    mock.register(
      get(urlPathEqualTo(s"/individuals/income/sa/nino/$nino"))
        .withQueryParam("startYear", equalTo(fromTaxYear))
        .withQueryParam("endYear", equalTo(toTaxYear))
        .withQueryParam("fields", equalTo(fields))
        .willReturn(aResponse().withStatus(Status.TOO_MANY_REQUESTS)))

}
