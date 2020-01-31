/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.domain

import java.util.UUID
import play.api.libs.json._
import scala.util.{Failure, Try}
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

object JsonFormatters {

  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

  implicit val errorInvalidRequestFormat = new Format[ErrorInvalidRequest] {
    def reads(json: JsValue): JsResult[ErrorInvalidRequest] = JsSuccess(
      ErrorInvalidRequest((json \ "message").as[String])
    )

    def writes(error: ErrorInvalidRequest): JsValue =
      Json.obj("code" -> error.errorCode, "message" -> error.message)
  }

  implicit val uuidJsonFormat = new Format[UUID] {
    override def writes(uuid: UUID) = JsString(uuid.toString)

    override def reads(json: JsValue) = JsSuccess(UUID.fromString(json.asInstanceOf[JsString].value))
  }

  implicit val formatTaxYear = new Format[TaxYear] {
    override def reads(json: JsValue): JsResult[TaxYear] = JsSuccess(TaxYear(json.asInstanceOf[JsString].value))
    override def writes(taxYear: TaxYear): JsValue = JsString(taxYear.formattedTaxYear)
  }

  implicit val paymentJsonFormat = Json.format[Payment]
  implicit val matchedCitizenJsonFormat = Json.format[MatchedCitizen]

  implicit val desPaymentJsonFormat = Json.format[DesPayment]
  implicit val desEmploymentPayFrequencyJsonFormat = EnumJson.enumFormat(DesEmploymentPayFrequency)
  implicit val desEmploymentJsonFormat = Json.format[DesEmployment]
  implicit val desEmploymentsJsonFormat = Json.format[DesEmployments]

  implicit val formatSaReturn = Json.format[SaSubmission]
  implicit val formatSaAnnualReturns = Json.format[SaTaxReturn]
  implicit val formatSaRegistration = Json.format[SaRegistration]
  implicit val formatSaFootprint = Json.format[SaFootprint]

  implicit val formatSaEmploymentsIncome = Json.format[SaEmploymentsIncome]
  implicit val formatSaAnnualEmployments = Json.format[SaAnnualEmployments]

  implicit val formatSaSelfEmploymentsIncome = Json.format[SaSelfEmploymentsIncome]
  implicit val formatSaAnnualSelfEmployments = Json.format[SaAnnualSelfEmployments]

  implicit val formatSaTaxReturnSummary = Json.format[SaTaxReturnSummary]
  implicit val formatSaTaxReturnSummaries = Json.format[SaTaxReturnSummaries]

  implicit val formatSaAnnualTrustIncome = Json.format[SaAnnualTrustIncome]
  implicit val formatSaAnnualTrustIncomes = Json.format[SaAnnualTrustIncomes]

  implicit val formatSaAnnualForeignIncome = Json.format[SaAnnualForeignIncome]
  implicit val formatSaAnnualForeignIncomes = Json.format[SaAnnualForeignIncomes]

  implicit val formatSaAnnualPartnershipIncome = Json.format[SaAnnualPartnershipIncome]
  implicit val formatSaAnnualPartnershipIncomes = Json.format[SaAnnualPartnershipIncomes]

  implicit val formatSaAnnualInterestAndDividendIncome = Json.format[SaAnnualInterestAndDividendIncome]
  implicit val formatSaAnnualInterestAndDividendIncomes = Json.format[SaAnnualInterestAndDividendIncomes]

  implicit val formatSaAnnualUkPropertiesIncome = Json.format[SaAnnualUkPropertiesIncome]
  implicit val formatSaAnnualUkPropertiesIncomes = Json.format[SaAnnualUkPropertiesIncomes]

  implicit val formatSaAnnualPensionAndStateBenefitIncome = Json.format[SaAnnualPensionAndStateBenefitIncome]
  implicit val formatSaAnnualPensionAndStateBenefitIncomes = Json.format[SaAnnualPensionAndStateBenefitIncomes]

  implicit val formatSaAnnualAdditionalInformation = Json.format[SaAnnualAdditionalInformation]
  implicit val formatSaAnnualAdditionalInformations = Json.format[SaAnnualAdditionalInformations]

  implicit val formatSaAnnualOtherIncome = Json.format[SaAnnualOtherIncome]
  implicit val formatSaAnnualOtherIncomes = Json.format[SaAnnualOtherIncomes]

}

object EnumJson {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) =>
        Try(JsSuccess(enum.withName(s))) recoverWith {
          case _: NoSuchElementException => Failure(new InvalidEnumException(enum.getClass.getSimpleName, s))
        } get
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] =
    Format(enumReads(enum), enumWrites)
}

class InvalidEnumException(className: String, input: String)
    extends RuntimeException(s"Enumeration expected of type: '$className', but it does not contain '$input'")
