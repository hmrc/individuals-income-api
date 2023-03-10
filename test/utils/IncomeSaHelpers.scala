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

package utils

import uk.gov.hmrc.individualsincomeapi.domain.integrationframework._

trait IncomeSaHelpers {
  def createValidSaTaxYearEntry() = {
    val returnTypeList = Seq(createValidSaReturnType())
    IfSaEntry(Some("2020"), Some(100.01), Some(returnTypeList))
  }

  def createValidSaTaxYearEntryNoDataContainers() = {
    val returnTypeList = Seq(createValidSaReturnTypeNoDataContainers())
    IfSaEntry(Some("2020"), Some(100.01), Some(returnTypeList))
  }

  def createValidSaTaxYearEntryNoValues() = {
    val returnTypeList = Seq(createValidSaReturnTypeNoValues())
    IfSaEntry(Some("2020"), None, Some(returnTypeList))
  }

  private def createValidSaReturnType() = {
    val validSaIncome = IfSaIncome(
      selfAssessment = Some(100.0),
      allEmployments = Some(100.0),
      ukInterest = Some(100.0),
      foreignDivs = Some(100.0),
      ukDivsAndInterest = Some(100.0),
      partnerships = Some(100.0),
      pensions = Some(100.0),
      selfEmployment = Some(100.0),
      trusts = Some(100.0),
      ukProperty = Some(100.0),
      foreign = Some(100.0),
      lifePolicies = Some(100.0),
      shares = Some(100.0),
      other = Some(100.0)
    )

    val validDeducts = IfDeducts(
      Some(200.0),
      Some(200.0)
    )

    IfSaReturn(
      Some("1234567890"),
      Some("2020-01-01"),
      Some("2020-01-01"),
      Some("This is a business description"),
      Some("12345678901"),
      Some("2020-01-01"),
      Some("2020-01-30"),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(IfAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), None, Some("QW123QW"))),
      Some(validSaIncome),
      Some(validDeducts)
    )
  }

  private def createValidSaReturnTypeNoDataContainers() = {
    val validSaIncome = None
    val validDeducts = None

    IfSaReturn(
      Some("1234567890"),
      Some("2020-01-01"),
      Some("2020-01-01"),
      Some("This is a business description"),
      Some("12345678901"),
      Some("2020-01-01"),
      Some("2020-01-30"),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(IfAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), None, Some("QW123QW"))),
      validSaIncome,
      validDeducts
    )
  }

  private def createValidSaReturnTypeNoValues() = {
    val validSaIncome = IfSaIncome(
      selfAssessment = None,
      allEmployments = None,
      ukInterest = None,
      foreignDivs = None,
      ukDivsAndInterest = None,
      partnerships = None,
      pensions = None,
      selfEmployment = None,
      trusts = None,
      ukProperty = None,
      foreign = None,
      lifePolicies = None,
      shares = None,
      other = None
    )

    IfSaReturn(
      Some("1234567890"),
      Some("2020-01-01"),
      Some("2020-01-01"),
      Some("This is a business description"),
      Some("12345678901"),
      Some("2020-01-01"),
      Some("2020-01-30"),
      None,
      None,
      None,
      None,
      None,
      Some(IfAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), None, Some("QW123QW"))),
      Some(validSaIncome),
      None
    )
  }
}
