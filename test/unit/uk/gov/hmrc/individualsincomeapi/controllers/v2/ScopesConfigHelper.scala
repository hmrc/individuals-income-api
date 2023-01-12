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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import play.api.Configuration

trait ScopesConfigHelper {

  val mockScopesConfig = Configuration(
    (s"api-config.scopes.test-scope.fields", Seq("A", "B", "C")),
    (s"api-config.scopes.test-scope-1.fields", Seq("D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q")),
    (s"api-config.endpoints.internal.paye.endpoint", "/individuals/income/paye?matchId=<matchId>{&fromDate,toDate}"),
    (s"api-config.endpoints.internal.paye.title", "Get an individual's income paye data"),
    (s"api-config.endpoints.internal.paye.fields", Seq("A", "B", "C")),

    (s"api-config.endpoints.internal.sa.endpoint", "/individuals/income/sa?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.sa.title", "Get an individual's income sa data"),
    (s"api-config.endpoints.internal.sa.fields", Seq("D")),
    (
      s"api-config.endpoints.internal.summary.endpoint",
      "/individuals/income/sa/summary?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.summary.title", "Get an individual's income sa summary data"),
    (s"api-config.endpoints.internal.summary.fields", Seq("E")),
    (
      s"api-config.endpoints.internal.trusts.endpoint",
      "/individuals/income/sa/trusts?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.trusts.title", "Get an individual's income sa trusts data"),
    (s"api-config.endpoints.internal.trusts.fields", Seq("F")),
    (
      s"api-config.endpoints.internal.foreign.endpoint",
      "/individuals/income/sa/foreign?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.foreign.title", "Get an individual's income sa foreign data"),
    (s"api-config.endpoints.internal.foreign.fields", Seq("G")),
    (
      s"api-config.endpoints.internal.partnerships.endpoint",
      "/individuals/income/sa/partnerships?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.partnerships.title", "Get an individual's income sa partnerships data"),
    (s"api-config.endpoints.internal.partnerships.fields", Seq("H")),
    (
      s"api-config.endpoints.internal.interestsAndDividends.endpoint",
      "/individuals/income/sa/interests-and-dividends?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.interestsAndDividends.title", "Get an individual's interests-and-dividends sa data"),
    (s"api-config.endpoints.internal.interestsAndDividends.fields", Seq("I")),
    (
      s"api-config.endpoints.internal.pensionsAndStateBenefits.endpoint",
      "/individuals/income/sa/pensions-and-state-benefits?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.pensionsAndStateBenefits.title", "Get an individual's pensions-and-state-benefits sa data"),
    (s"api-config.endpoints.internal.pensionsAndStateBenefits.fields", Seq("J")),
    (
      s"api-config.endpoints.internal.ukProperties.endpoint",
      "/individuals/income/sa/uk-properties?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.ukProperties.title", "Get an individual's uk-properties sa data"),
    (s"api-config.endpoints.internal.ukProperties.fields", Seq("K")),
    (
      s"api-config.endpoints.internal.additionalInformation.endpoint",
      "/individuals/income/sa/additional-information?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.additionalInformation.title", "Get an individual's additional-information sa data"),
    (s"api-config.endpoints.internal.additionalInformation.fields", Seq("L")),
    (s"api-config.endpoints.internal.other.endpoint", "/individuals/income/sa/other?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.other.title", "Get an individual's other sa data"),
    (s"api-config.endpoints.internal.other.fields", Seq("M")),
    (
      s"api-config.endpoints.internal.source.endpoint",
      "/individuals/income/sa/source?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.source.title", "Get an individual's source sa data"),
    (s"api-config.endpoints.internal.source.fields", Seq("N")),
    (
      s"api-config.endpoints.internal.employments.endpoint",
      "/individuals/income/sa/employments?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.employments.title", "Get an individual's employments sa data"),
    (s"api-config.endpoints.internal.employments.fields", Seq("O")),
    (
      s"api-config.endpoints.internal.selfEmployments.endpoint",
      "/individuals/income/sa/self-employments?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.selfEmployments.title", "Get an individual's self-employments sa data"),
    (s"api-config.endpoints.internal.selfEmployments.fields", Seq("P")),
    (
      s"api-config.endpoints.internal.furtherDetails.endpoint",
      "/individuals/income/sa/further-details?matchId=<matchId>{&fromTaxYear,toTaxYear}"),
    (s"api-config.endpoints.internal.furtherDetails.title", "Get an individual's further-details sa data"),
    (s"api-config.endpoints.internal.furtherDetails.fields", Seq("Q")),

    (s"api-config.fields.A", "foo/bar/one"),
    (s"api-config.fields.B", "foo/bar/one"),
    (s"api-config.fields.C", "foo/bar/one"),
    (s"api-config.fields.D", "foo/bar/one"),
    (s"api-config.fields.E", "foo/bar/one"),
    (s"api-config.fields.F", "foo/bar/one"),
    (s"api-config.fields.G", "foo/bar/one"),
    (s"api-config.fields.H", "foo/bar/one"),
    (s"api-config.fields.I", "foo/bar/one"),
    (s"api-config.fields.J", "foo/bar/one"),
    (s"api-config.fields.K", "foo/bar/one"),
    (s"api-config.fields.L", "foo/bar/one"),
    (s"api-config.fields.M", "foo/bar/one"),
    (s"api-config.fields.N", "foo/bar/one"),
    (s"api-config.fields.O", "foo/bar/one"),
    (s"api-config.fields.P", "foo/bar/one"),
    (s"api-config.fields.Q", "foo/bar/one")
  )

}
