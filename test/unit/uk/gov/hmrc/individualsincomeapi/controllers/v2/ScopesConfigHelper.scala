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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import play.api.Configuration

trait ScopesConfigHelper {

  val mockScopesConfig = Configuration(
    (s"api-config.scopes.test-scope.fields", List("A", "B", "C")),
    (
      s"api-config.scopes.test-scope-1.fields",
      List("D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q")),
    (s"api-config.endpoints.incomePaye.endpoint", "/individuals/income/paye?matchId=<matchId>{&fromDate,toDate}"),
    (s"api-config.endpoints.incomePaye.title", "Get an individual's income paye data"),
    (s"api-config.endpoints.incomePaye.fields.A", "foo/bar/one"),
    (s"api-config.endpoints.incomePaye.fields.B", "foo/bar/one"),
    (s"api-config.endpoints.incomePaye.fields.C", "foo/bar/one"),
    (s"api-config.endpoints.incomeSa.endpoint", "/individuals/income/sa?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSa.title", "Get an individual's income sa data"),
    (s"api-config.endpoints.incomeSa.fields.D", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaSummary.endpoint",
      "/individuals/income/sa/summary?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaSummary.title", "Get an individual's income sa summary data"),
    (s"api-config.endpoints.incomeSaSummary.fields.E", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaTrusts.endpoint",
      "/individuals/income/sa/trusts?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaTrusts.title", "Get an individual's income sa trusts data"),
    (s"api-config.endpoints.incomeSaTrusts.fields.F", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaForeign.endpoint",
      "/individuals/income/sa/foreign?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaForeign.title", "Get an individual's income sa foreign data"),
    (s"api-config.endpoints.incomeSaForeign.fields.G", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaPartnerships.endpoint",
      "/individuals/income/sa/partnerships?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaPartnerships.title", "Get an individual's income sa partnerships data"),
    (s"api-config.endpoints.incomeSaPartnerships.fields.H", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaInterestsAndDividends.endpoint",
      "/individuals/income/sa/interests-and-dividends?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (
      s"api-config.endpoints.incomeSaInterestsAndDividends.title",
      "Get an individual's interests-and-dividends sa data"),
    (s"api-config.endpoints.incomeSaInterestsAndDividends.fields.I", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaPensionsAndStateBenefits.endpoint",
      "/individuals/income/sa/pensions-and-state-benefits?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (
      s"api-config.endpoints.incomeSaPensionsAndStateBenefits.title",
      "Get an individual's pensions-and-state-benefits sa data"),
    (s"api-config.endpoints.incomeSaPensionsAndStateBenefits.fields.J", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaUkProperties.endpoint",
      "/individuals/income/sa/uk-properties?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaUkProperties.title", "Get an individual's uk-properties sa data"),
    (s"api-config.endpoints.incomeSaUkProperties.fields.K", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaAdditionalInformation.endpoint",
      "/individuals/income/sa/additional-information?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaAdditionalInformation.title", "Get an individual's additional-information sa data"),
    (s"api-config.endpoints.incomeSaAdditionalInformation.fields.L", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaOther.endpoint",
      "/individuals/income/sa/other?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaOther.title", "Get an individual's other sa data"),
    (s"api-config.endpoints.incomeSaOther.fields.M", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaSource.endpoint",
      "/individuals/income/sa/source?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaSource.title", "Get an individual's source sa data"),
    (s"api-config.endpoints.incomeSaSource.fields.N", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaEmployments.endpoint",
      "/individuals/income/sa/employments?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaEmployments.title", "Get an individual's employments sa data"),
    (s"api-config.endpoints.incomeSaEmployments.fields.O", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaSelfEmployments.endpoint",
      "/individuals/income/sa/self-employments?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaSelfEmployments.title", "Get an individual's self-employments sa data"),
    (s"api-config.endpoints.incomeSaSelfEmployments.fields.P", "foo/bar/one"),
    (
      s"api-config.endpoints.incomeSaFurtherDetails.endpoint",
      "/individuals/income/sa/further-details?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.incomeSaFurtherDetails.title", "Get an individual's further-details sa data"),
    (s"api-config.endpoints.incomeSaFurtherDetails.fields.Q", "foo/bar/one")
  )
}
