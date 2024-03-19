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

package uk.gov.hmrc.individualsincomeapi.domain.v1

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.controllers.RestFormats
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.des.DesSAIncome

case class SaFootprint(registrations: Seq[SaRegistration], taxReturns: Seq[SaTaxReturn])

case class SaTaxReturn(taxYear: TaxYear, submissions: Seq[SaSubmission])

case class SaSubmission(utr: SaUtr, receivedDate: Option[LocalDate])

case class SaRegistration(utr: SaUtr, registrationDate: Option[LocalDate])

object SaTaxReturn {
  def apply(desSaIncome: DesSAIncome): SaTaxReturn =
    SaTaxReturn(
      TaxYear.fromEndYear(desSaIncome.taxYear.toInt),
      desSaIncome.returnList.map(r => SaSubmission(r.utr, r.receivedDate)))
}

object SaFootprint {
  implicit def dateTimeOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def apply(desSaIncomes: Seq[DesSAIncome]): SaFootprint = {
    val saRegistrations =
      desSaIncomes.flatMap(_.returnList map (saReturn => SaRegistration(saReturn.utr, saReturn.caseStartDate))).toSet
    val saReturns = desSaIncomes.sortBy(_.taxYear.toInt).reverse map (r => SaTaxReturn(r))

    SaFootprint(saRegistrations.toSeq.sortBy(_.registrationDate).reverse, saReturns)
  }
}

case class SaAnnualEmployments(taxYear: TaxYear, employments: Seq[SaEmploymentsIncome])

case class SaEmploymentsIncome(utr: SaUtr, employmentIncome: Double)

object SaAnnualEmployments {
  def apply(desSaIncome: DesSAIncome): SaAnnualEmployments =
    SaAnnualEmployments(
      TaxYear.fromEndYear(desSaIncome.taxYear.toInt),
      desSaIncome.returnList.map(sa => SaEmploymentsIncome(sa.utr, sa.income.incomeFromAllEmployments.getOrElse(0.0)))
    )
}

case class SaAnnualSelfEmployments(taxYear: TaxYear, selfEmployments: Seq[SaSelfEmploymentsIncome])

case class SaSelfEmploymentsIncome(utr: SaUtr, selfEmploymentProfit: Double)

object SaAnnualSelfEmployments {
  def apply(desSAIncome: DesSAIncome): SaAnnualSelfEmployments =
    SaAnnualSelfEmployments(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa =>
        SaSelfEmploymentsIncome(sa.utr, sa.income.profitFromSelfEmployment.getOrElse(0.0)))
    )
}

case class SaTaxReturnSummaries(taxYear: TaxYear, summary: Seq[SaTaxReturnSummary])

case class SaTaxReturnSummary(utr: SaUtr, totalIncome: Double)

object SaTaxReturnSummaries {
  def apply(desSAIncome: DesSAIncome): SaTaxReturnSummaries =
    SaTaxReturnSummaries(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa => SaTaxReturnSummary(sa.utr, sa.income.incomeFromSelfAssessment.getOrElse(0.0)))
    )
}

case class SaAnnualTrustIncomes(taxYear: TaxYear, trusts: Seq[SaAnnualTrustIncome])

case class SaAnnualTrustIncome(utr: SaUtr, trustIncome: Double)

object SaAnnualTrustIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualTrustIncomes =
    SaAnnualTrustIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa => SaAnnualTrustIncome(sa.utr, sa.income.incomeFromTrust.getOrElse(0.0)))
    )
}

case class SaAnnualForeignIncomes(taxYear: TaxYear, foreign: Seq[SaAnnualForeignIncome])

case class SaAnnualForeignIncome(utr: SaUtr, foreignIncome: Double)

object SaAnnualForeignIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualForeignIncomes =
    SaAnnualForeignIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa =>
        SaAnnualForeignIncome(sa.utr, sa.income.incomeFromForeign4Sources.getOrElse(0.0)))
    )
}

case class SaAnnualUkPropertiesIncomes(taxYear: TaxYear, ukProperties: Seq[SaAnnualUkPropertiesIncome])

case class SaAnnualUkPropertiesIncome(utr: SaUtr, totalProfit: Double)

object SaAnnualUkPropertiesIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualUkPropertiesIncomes =
    SaAnnualUkPropertiesIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa => SaAnnualUkPropertiesIncome(sa.utr, sa.income.incomeFromProperty.getOrElse(0.0)))
    )
}

case class SaAnnualAdditionalInformations(taxYear: TaxYear, additionalInformation: Seq[SaAnnualAdditionalInformation])

case class SaAnnualAdditionalInformation(utr: SaUtr, gainsOnLifePolicies: Double, sharesOptionsIncome: Double)

object SaAnnualAdditionalInformations {
  def apply(desSAIncome: DesSAIncome): SaAnnualAdditionalInformations =
    SaAnnualAdditionalInformations(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(
        sa =>
          SaAnnualAdditionalInformation(
            sa.utr,
            sa.income.incomeFromGainsOnLifePolicies.getOrElse(0.0),
            sa.income.incomeFromSharesOptions.getOrElse(0.0)))
    )
}

case class SaAnnualPartnershipIncomes(taxYear: TaxYear, partnerships: Seq[SaAnnualPartnershipIncome])

case class SaAnnualPartnershipIncome(utr: SaUtr, partnershipProfit: Double)

object SaAnnualPartnershipIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualPartnershipIncomes =
    SaAnnualPartnershipIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa =>
        SaAnnualPartnershipIncome(sa.utr, sa.income.profitFromPartnerships.getOrElse(0.0)))
    )
}

case class SaAnnualInterestAndDividendIncomes(
  taxYear: TaxYear,
  interestsAndDividends: Seq[SaAnnualInterestAndDividendIncome])

case class SaAnnualInterestAndDividendIncome(
  utr: SaUtr,
  ukInterestsIncome: Double,
  foreignDividendsIncome: Double,
  ukDividendsIncome: Double)

object SaAnnualInterestAndDividendIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualInterestAndDividendIncomes =
    SaAnnualInterestAndDividendIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(
        sa =>
          SaAnnualInterestAndDividendIncome(
            sa.utr,
            sa.income.incomeFromUkInterest.getOrElse(0.0),
            sa.income.incomeFromForeignDividends.getOrElse(0.0),
            sa.income.incomeFromInterestNDividendsFromUKCompaniesNTrusts.getOrElse(0.0)
        ))
    )
}

case class SaAnnualPensionAndStateBenefitIncomes(
  taxYear: TaxYear,
  pensionsAndStateBenefits: Seq[SaAnnualPensionAndStateBenefitIncome])

case class SaAnnualPensionAndStateBenefitIncome(utr: SaUtr, totalIncome: Double)

object SaAnnualPensionAndStateBenefitIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualPensionAndStateBenefitIncomes =
    SaAnnualPensionAndStateBenefitIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa =>
        SaAnnualPensionAndStateBenefitIncome(sa.utr, sa.income.incomeFromPensions.getOrElse(0.0)))
    )
}

case class SaAnnualOtherIncomes(taxYear: TaxYear, other: Seq[SaAnnualOtherIncome])

case class SaAnnualOtherIncome(utr: SaUtr, otherIncome: Double)

object SaAnnualOtherIncomes {
  def apply(desSAIncome: DesSAIncome): SaAnnualOtherIncomes =
    SaAnnualOtherIncomes(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList.map(sa => SaAnnualOtherIncome(sa.utr, sa.income.incomeFromOther.getOrElse(0.0)))
    )
}

case class SaIncomeSources(taxYear: TaxYear, sources: Seq[SaIncomeSource])

object SaIncomeSources {
  def apply(desSAIncome: DesSAIncome): SaIncomeSources =
    SaIncomeSources(
      TaxYear.fromEndYear(desSAIncome.taxYear.toInt),
      desSAIncome.returnList
        .map { sa =>
          val addressType = sa.addressTypeIndicator match {
            case Some("B") => Some("homeAddress")
            case Some("C") => Some("correspondenceAddress")
            case Some(_)   => Some("other")
            case None      => None
          }

          val address = SourceAddress(
            sa.addressLine1,
            sa.addressLine2,
            sa.addressLine3,
            sa.addressLine4,
            line5 = None,
            sa.postalCode,
            sa.baseAddressEffectiveDate,
            addressType
          )

          SaIncomeSource(
            sa.utr,
            sa.businessDescription,
            Some(address).filterNot(_.isEmpty),
            sa.telephoneNumber
          )
        }
        .filter(s => s.businessAddress.isDefined || s.businessDescription.isDefined)
    )

  implicit val format: Format[SaIncomeSources] = Json.format[SaIncomeSources]
}

case class SourceAddress(
  line1: Option[String] = None,
  line2: Option[String] = None,
  line3: Option[String] = None,
  line4: Option[String] = None,
  line5: Option[String] = None,
  postcode: Option[String] = None,
  effectiveDate: Option[LocalDate] = None,
  addressType: Option[String] = None) {

  def isEmpty: Boolean = this == SourceAddress()
}

object SourceAddress {
  implicit val apiWrites: Format[SourceAddress] = Json.format[SourceAddress]
}

case class SaIncomeSource(
  utr: SaUtr,
  businessDescription: Option[String],
  businessAddress: Option[SourceAddress],
  telephoneNumber: Option[String])

object SaIncomeSource {
  implicit val format: Format[SaIncomeSource] = Json.format[SaIncomeSource]
}
