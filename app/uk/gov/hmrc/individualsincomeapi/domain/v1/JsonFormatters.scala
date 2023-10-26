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
import play.api.libs.json._
import uk.gov.hmrc.http.controllers.RestFormats
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesEmployment, DesEmploymentPayFrequency, DesEmployments, DesPayment}
import uk.gov.hmrc.individualsincomeapi.domain.{ErrorInvalidRequest, ErrorResponse}

import java.util.UUID
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Try}

object JsonFormatters {

  implicit val dateFormat: Format[LocalDate] = RestFormats.localDateFormats

  implicit val errorResponseWrites: Writes[ErrorResponse] = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

  implicit val errorInvalidRequestFormat: Format[ErrorInvalidRequest] = new Format[ErrorInvalidRequest] {
    def reads(json: JsValue): JsResult[ErrorInvalidRequest] = JsSuccess(
      ErrorInvalidRequest((json \ "message").as[String])
    )

    def writes(error: ErrorInvalidRequest): JsValue =
      Json.obj("code" -> error.errorCode, "message" -> error.message)
  }

  implicit val uuidJsonFormat: Format[UUID] = new Format[UUID] {
    override def writes(uuid: UUID): JsString = JsString(uuid.toString)

    override def reads(json: JsValue): JsSuccess[UUID] = JsSuccess(UUID.fromString(json.asInstanceOf[JsString].value))
  }

  implicit val matchedCitizenJsonFormat: OFormat[MatchedCitizen] = Json.format[MatchedCitizen]

  implicit val desPaymentJsonFormat: OFormat[DesPayment] = Json.format[DesPayment]
  implicit val desEmploymentPayFrequencyJsonFormat: Format[DesEmploymentPayFrequency.Value] = EnumJson.enumFormat(DesEmploymentPayFrequency)
  implicit val desEmploymentJsonFormat: OFormat[DesEmployment] = Json.format[DesEmployment]
  implicit val desEmploymentsJsonFormat: OFormat[DesEmployments] = Json.format[DesEmployments]

  implicit val formatSaReturn: OFormat[SaSubmission] = Json.format[SaSubmission]
  implicit val formatSaAnnualReturns: OFormat[SaTaxReturn] = Json.format[SaTaxReturn]
  implicit val formatSaRegistration: OFormat[SaRegistration] = Json.format[SaRegistration]
  implicit val formatSaFootprint: OFormat[SaFootprint] = Json.format[SaFootprint]

  implicit val formatSaEmploymentsIncome: OFormat[SaEmploymentsIncome] = Json.format[SaEmploymentsIncome]
  implicit val formatSaAnnualEmployments: OFormat[SaAnnualEmployments] = Json.format[SaAnnualEmployments]

  implicit val formatSaSelfEmploymentsIncome: OFormat[SaSelfEmploymentsIncome] = Json.format[SaSelfEmploymentsIncome]
  implicit val formatSaAnnualSelfEmployments: OFormat[SaAnnualSelfEmployments] = Json.format[SaAnnualSelfEmployments]

  implicit val formatSaTaxReturnSummary: OFormat[SaTaxReturnSummary] = Json.format[SaTaxReturnSummary]
  implicit val formatSaTaxReturnSummaries: OFormat[SaTaxReturnSummaries] = Json.format[SaTaxReturnSummaries]

  implicit val formatSaAnnualTrustIncome: OFormat[SaAnnualTrustIncome] = Json.format[SaAnnualTrustIncome]
  implicit val formatSaAnnualTrustIncomes: OFormat[SaAnnualTrustIncomes] = Json.format[SaAnnualTrustIncomes]

  implicit val formatSaAnnualForeignIncome: OFormat[SaAnnualForeignIncome] = Json.format[SaAnnualForeignIncome]
  implicit val formatSaAnnualForeignIncomes: OFormat[SaAnnualForeignIncomes] = Json.format[SaAnnualForeignIncomes]

  implicit val formatSaAnnualPartnershipIncome: OFormat[SaAnnualPartnershipIncome] = Json.format[SaAnnualPartnershipIncome]
  implicit val formatSaAnnualPartnershipIncomes: OFormat[SaAnnualPartnershipIncomes] = Json.format[SaAnnualPartnershipIncomes]

  implicit val formatSaAnnualInterestAndDividendIncome: OFormat[SaAnnualInterestAndDividendIncome] = Json.format[SaAnnualInterestAndDividendIncome]
  implicit val formatSaAnnualInterestAndDividendIncomes: OFormat[SaAnnualInterestAndDividendIncomes] = Json.format[SaAnnualInterestAndDividendIncomes]

  implicit val formatSaAnnualUkPropertiesIncome: OFormat[SaAnnualUkPropertiesIncome] = Json.format[SaAnnualUkPropertiesIncome]
  implicit val formatSaAnnualUkPropertiesIncomes: OFormat[SaAnnualUkPropertiesIncomes] = Json.format[SaAnnualUkPropertiesIncomes]

  implicit val formatSaAnnualPensionAndStateBenefitIncome: OFormat[SaAnnualPensionAndStateBenefitIncome] = Json.format[SaAnnualPensionAndStateBenefitIncome]
  implicit val formatSaAnnualPensionAndStateBenefitIncomes: OFormat[SaAnnualPensionAndStateBenefitIncomes] = Json.format[SaAnnualPensionAndStateBenefitIncomes]

  implicit val formatSaAnnualAdditionalInformation: OFormat[SaAnnualAdditionalInformation] = Json.format[SaAnnualAdditionalInformation]
  implicit val formatSaAnnualAdditionalInformations: OFormat[SaAnnualAdditionalInformations] = Json.format[SaAnnualAdditionalInformations]

  implicit val formatSaAnnualOtherIncome: OFormat[SaAnnualOtherIncome] = Json.format[SaAnnualOtherIncome]
  implicit val formatSaAnnualOtherIncomes: OFormat[SaAnnualOtherIncomes] = Json.format[SaAnnualOtherIncomes]
}

object EnumJson {

  private def enumReads[E <: Enumeration](anEnum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) =>
        Try(JsSuccess(anEnum.withName(s))) recoverWith {
          case _: NoSuchElementException => Failure(new InvalidEnumException(anEnum.getClass.getSimpleName, s))
        } get
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](anEnum: E): Format[E#Value] =
    Format(enumReads(anEnum), enumWrites)
}

class InvalidEnumException(className: String, input: String)
  extends RuntimeException(s"Enumeration expected of type: '$className', but it does not contain '$input'")
