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

package uk.gov.hmrc.individualsincomeapi.domain.des

import java.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval

case class DesSAIncome(taxYear: String, returnList: Seq[DesSAReturn]) {

  def isIn(taxYearInterval: TaxYearInterval) =
    taxYear.toInt >= taxYearInterval.fromTaxYear.endYr && taxYear.toInt <= taxYearInterval.toTaxYear.endYr
}

object DesSAIncome {
  implicit val format: Format[DesSAIncome] = Json.format[DesSAIncome]

  val desReads: Reads[DesSAIncome] = {
    implicit val saReturnReads: Reads[DesSAReturn] = DesSAReturn.desReads

    Json.reads[DesSAIncome]
  }
}

case class DesSAReturn(
  caseStartDate: Option[LocalDate],
  receivedDate: Option[LocalDate],
  utr: SaUtr,
  income: SAIncome = SAIncome(),
  businessDescription: Option[String] = None,
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[String] = None,
  telephoneNumber: Option[String] = None,
  baseAddressEffectiveDate: Option[LocalDate] = None,
  addressTypeIndicator: Option[String] = None)

object DesSAReturn {
  implicit val format: Format[DesSAReturn] = new Format[DesSAReturn] {
    override def writes(o: DesSAReturn): JsValue =
      Json.obj(
        "caseStartDate"            -> o.caseStartDate,
        "receivedDate"             -> o.receivedDate,
        "utr"                      -> o.utr,
        "businessDescription"      -> o.businessDescription,
        "addressLine1"             -> o.addressLine1,
        "addressLine2"             -> o.addressLine2,
        "addressLine3"             -> o.addressLine3,
        "addressLine4"             -> o.addressLine4,
        "postalCode"               -> o.postalCode,
        "telephoneNumber"          -> o.telephoneNumber,
        "baseAddressEffectiveDate" -> o.baseAddressEffectiveDate,
        "addressTypeIndicator"     -> o.addressTypeIndicator
      ) ++ Json.toJson(o.income).as[JsObject]

    override def reads(json: JsValue): JsResult[DesSAReturn] =
      for {
        caseStartDate            <- (json \ "caseStartDate").validateOpt[LocalDate]
        receivedDate             <- (json \ "receivedDate").validateOpt[LocalDate]
        utr                      <- (json \ "utr").validate[SaUtr]
        income                   <- json.validate[SAIncome]
        businessDescription      <- (json \ "businessDescription").validateOpt[String]
        addressLine1             <- (json \ "addressLine1").validateOpt[String]
        addressLine2             <- (json \ "addressLine2").validateOpt[String]
        addressLine3             <- (json \ "addressLine3").validateOpt[String]
        addressLine4             <- (json \ "addressLine4").validateOpt[String]
        postalCode               <- (json \ "postalCode").validateOpt[String]
        telephoneNumber          <- (json \ "telephoneNumber").validateOpt[String]
        baseAddressEffectiveDate <- (json \ "baseAddressEffectiveDate").validateOpt[LocalDate]
        addressTypeIndicator     <- (json \ "addressTypeIndicator").validateOpt[String]
      } yield
        DesSAReturn(
          caseStartDate,
          receivedDate,
          utr,
          income,
          businessDescription,
          addressLine1,
          addressLine2,
          addressLine3,
          addressLine4,
          postalCode,
          telephoneNumber,
          baseAddressEffectiveDate,
          addressTypeIndicator
        )
  }

  val desReads: Reads[DesSAReturn] = new Reads[DesSAReturn] {
    override def reads(json: JsValue): JsResult[DesSAReturn] =
      for {
        caseStartDate       <- (json \ "caseStartDate").validateOpt[LocalDate]
        receivedDate        <- (json \ "receivedDate").validateOpt[LocalDate]
        utr                 <- (json \ "utr").validate[SaUtr]
        income              <- json.validate[SAIncome]
        businessDescription <- (json \ "businessDescription").validateOpt[String]
        addressLine1        <- (json \ "addressLine1").validateOpt[String]
        addressLine2        <- (json \ "addressLine2").validateOpt[String]
        addressLine3        <- (json \ "addressLine3").validateOpt[String]
        addressLine4        <- (json \ "addressLine4").validateOpt[String]
        postalCode          <- (json \ "postalCode").validateOpt[String]
        telephoneNumber     <- (json \ "telephoneNumber").validateOpt[String]
        baseAddressEffectiveDate <- (json \ "baseAddressEffectivetDate")
                                     .validateOpt[LocalDate] // misspelled as per the DES Spec
        addressTypeIndicator <- (json \ "addressTypeIndicator").validateOpt[String]
      } yield
        DesSAReturn(
          caseStartDate,
          receivedDate,
          utr,
          income,
          businessDescription,
          addressLine1,
          addressLine2,
          addressLine3,
          addressLine4,
          postalCode,
          telephoneNumber,
          baseAddressEffectiveDate,
          addressTypeIndicator
        )
  }
}

case class SAIncome(
  incomeFromAllEmployments: Option[Double] = None,
  profitFromSelfEmployment: Option[Double] = None,
  incomeFromSelfAssessment: Option[Double] = None,
  incomeFromTrust: Option[Double] = None,
  incomeFromForeign4Sources: Option[Double] = None,
  profitFromPartnerships: Option[Double] = None,
  incomeFromUkInterest: Option[Double] = None,
  incomeFromForeignDividends: Option[Double] = None,
  incomeFromInterestNDividendsFromUKCompaniesNTrusts: Option[Double] = None,
  incomeFromProperty: Option[Double] = None,
  incomeFromPensions: Option[Double] = None,
  incomeFromSharesOptions: Option[Double] = None,
  incomeFromGainsOnLifePolicies: Option[Double] = None,
  incomeFromOther: Option[Double] = None)

object SAIncome {
  implicit val format: Format[SAIncome] = Json.format[SAIncome]
}
