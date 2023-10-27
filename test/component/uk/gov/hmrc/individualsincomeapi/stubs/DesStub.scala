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
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesEmployments, DesSAIncome}
import uk.gov.hmrc.individualsincomeapi.domain.v1.JsonFormatters._

object DesStub extends MockHost(23000) {

  def searchEmploymentIncomeForPeriodReturns(
    nino: String,
    fromDate: String,
    toDate: String,
    desEmployments: DesEmployments) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/nino/$nino/employments/income"))
        .withQueryParam("from", equalTo(fromDate))
        .withQueryParam("to", equalTo(toDate))
        .willReturn(aResponse().withStatus(Status.OK).withBody(Json.toJson(desEmployments).toString())))

  def searchEmploymentIncomeReturnsNoIncomeFor(nino: String, fromDate: String, toDate: String) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/nino/$nino/employments/income"))
        .withQueryParam("from", equalTo(fromDate))
        .withQueryParam("to", equalTo(toDate))
        .willReturn(aResponse().withStatus(Status.NOT_FOUND)))

  def searchEmploymentIncomeReturnsRateLimitErrorFor(nino: String, fromDate: String, toDate: String): Unit =
    mock.register(
      get(urlPathEqualTo(s"/individuals/nino/$nino/employments/income"))
        .withQueryParam("from", equalTo(fromDate))
        .withQueryParam("to", equalTo(toDate))
        .willReturn(aResponse().withStatus(Status.TOO_MANY_REQUESTS)))

  def searchSelfAssessmentIncomeForPeriodReturnsRateLimitErrorFor(
    nino: Nino,
    startYear: TaxYear,
    endYear: TaxYear,
    clientId: String,
    desSAIncomes: Seq[DesSAIncome]) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/nino/$nino/self-assessment/income"))
        .withHeader("OriginatorId", equalTo(s"MDTP_CLIENTID=$clientId"))
        .withQueryParam("startYear", equalTo(startYear.endYr.toString))
        .withQueryParam("endYear", equalTo(endYear.endYr.toString))
        .willReturn(aResponse().withStatus(Status.TOO_MANY_REQUESTS)))

  def searchSelfAssessmentIncomeForPeriodReturns(
    nino: Nino,
    startYear: TaxYear,
    endYear: TaxYear,
    clientId: String,
    desSAIncomes: Seq[DesSAIncome]) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/nino/$nino/self-assessment/income"))
        .withHeader("OriginatorId", equalTo(s"MDTP_CLIENTID=$clientId"))
        .withQueryParam("startYear", equalTo(startYear.endYr.toString))
        .withQueryParam("endYear", equalTo(endYear.endYr.toString))
        .willReturn(aResponse().withStatus(Status.OK).withBody(Json.toJson(desSAIncomes).toString())))

  def searchSelfAssessmentIncomeForPeriodReturnsNoDataFor(nino: String, startYear: String, endYear: String) =
    mock.register(
      get(urlPathEqualTo(s"/individuals/nino/$nino/self-assessment/income"))
        .withQueryParam("startYear", equalTo(startYear))
        .withQueryParam("endYear", equalTo(endYear))
        .willReturn(aResponse().withStatus(Status.NOT_FOUND)))

}
