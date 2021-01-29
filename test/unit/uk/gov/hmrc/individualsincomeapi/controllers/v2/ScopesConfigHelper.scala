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
    (s"api-config.endpoints.paye.endpoint", "/individuals/income/paye?matchId=<matchId>{&fromDate,toDate}"),
    (s"api-config.endpoints.paye.title", "Get an individual's income paye data"),
    (s"api-config.endpoints.paye.fields.A", "foo/bar/one"),
    (s"api-config.endpoints.paye.fields.B", "foo/bar/one"),
    (s"api-config.endpoints.paye.fields.C", "foo/bar/one"),
    (s"api-config.endpoints.sa.endpoint", "/individuals/income/sa?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.sa.title", "Get an individual's income sa data"),
    (s"api-config.endpoints.sa.fields.D", "foo/bar/one"),
    (
      s"api-config.endpoints.summary.endpoint",
      "/individuals/income/sa/summary?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.summary.title", "Get an individual's income sa summary data"),
    (s"api-config.endpoints.summary.fields.E", "foo/bar/one"),
    (
      s"api-config.endpoints.trusts.endpoint",
      "/individuals/income/sa/trusts?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.trusts.title", "Get an individual's income sa trusts data"),
    (s"api-config.endpoints.trusts.fields.F", "foo/bar/one"),
    (
      s"api-config.endpoints.foreign.endpoint",
      "/individuals/income/sa/foreign?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.foreign.title", "Get an individual's income sa foreign data"),
    (s"api-config.endpoints.foreign.fields.G", "foo/bar/one"),
    (
      s"api-config.endpoints.partnerships.endpoint",
      "/individuals/income/sa/partnerships?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.partnerships.title", "Get an individual's income sa partnerships data"),
    (s"api-config.endpoints.partnerships.fields.H", "foo/bar/one"),
    (
      s"api-config.endpoints.interestsAndDividends.endpoint",
      "/individuals/income/sa/interests-and-dividends?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.interestsAndDividends.title", "Get an individual's interests-and-dividends sa data"),
    (s"api-config.endpoints.interestsAndDividends.fields.I", "foo/bar/one"),
    (
      s"api-config.endpoints.pensionsAndStateBenefits.endpoint",
      "/individuals/income/sa/pensions-and-state-benefits?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.pensionsAndStateBenefits.title", "Get an individual's pensions-and-state-benefits sa data"),
    (s"api-config.endpoints.pensionsAndStateBenefits.fields.J", "foo/bar/one"),
    (
      s"api-config.endpoints.ukProperties.endpoint",
      "/individuals/income/sa/uk-properties?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.ukProperties.title", "Get an individual's uk-properties sa data"),
    (s"api-config.endpoints.ukProperties.fields.K", "foo/bar/one"),
    (
      s"api-config.endpoints.additionalInformation.endpoint",
      "/individuals/income/sa/additional-information?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.additionalInformation.title", "Get an individual's additional-information sa data"),
    (s"api-config.endpoints.additionalInformation.fields.L", "foo/bar/one"),
    (s"api-config.endpoints.other.endpoint", "/individuals/income/sa/other?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.other.title", "Get an individual's other sa data"),
    (s"api-config.endpoints.other.fields.M", "foo/bar/one"),
    (
      s"api-config.endpoints.source.endpoint",
      "/individuals/income/sa/source?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.source.title", "Get an individual's source sa data"),
    (s"api-config.endpoints.source.fields.N", "foo/bar/one"),
    (
      s"api-config.endpoints.employments.endpoint",
      "/individuals/income/sa/employments?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.employments.title", "Get an individual's employments sa data"),
    (s"api-config.endpoints.employments.fields.O", "foo/bar/one"),
    (
      s"api-config.endpoints.selfEmployments.endpoint",
      "/individuals/income/sa/self-employments?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.selfEmployments.title", "Get an individual's self-employments sa data"),
    (s"api-config.endpoints.selfEmployments.fields.P", "foo/bar/one"),
    (
      s"api-config.endpoints.furtherDetails.endpoint",
      "/individuals/income/sa/further-details?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.furtherDetails.title", "Get an individual's further-details sa data"),
    (s"api-config.endpoints.furtherDetails.fields.Q", "foo/bar/one")
  )
}
