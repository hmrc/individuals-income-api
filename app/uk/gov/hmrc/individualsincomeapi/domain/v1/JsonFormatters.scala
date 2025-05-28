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

import play.api.libs.json._
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesEmployment, DesEmploymentPayFrequency, DesEmployments, DesPayment}
import uk.gov.hmrc.individualsincomeapi.domain.{ErrorInvalidRequest, ErrorResponse}

import java.util.UUID
import scala.language.{implicitConversions, postfixOps}

object JsonFormatters {

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

  implicit val matchedCitizenJsonFormat: Format[MatchedCitizen] = Json.format[MatchedCitizen]

  implicit val desPaymentJsonFormat: Format[DesPayment] = Json.format[DesPayment]
  implicit val desEmploymentPayFrequencyJsonFormat: Format[DesEmploymentPayFrequency.Value] =
    Json.formatEnum(DesEmploymentPayFrequency)
  implicit val desEmploymentJsonFormat: Format[DesEmployment] = Json.format[DesEmployment]
  implicit val desEmploymentsJsonFormat: Format[DesEmployments] = Json.format[DesEmployments]

  implicit val formatSaReturn: Format[SaSubmission] = Json.format[SaSubmission]
  implicit val formatSaAnnualReturns: Format[SaTaxReturn] = Json.format[SaTaxReturn]
  implicit val formatSaRegistration: Format[SaRegistration] = Json.format[SaRegistration]
  implicit val formatSaFootprint: Format[SaFootprint] = Json.format[SaFootprint]

  implicit val formatSaEmploymentsIncome: Format[SaEmploymentsIncome] = Json.format[SaEmploymentsIncome]
  implicit val formatSaAnnualEmployments: Format[SaAnnualEmployments] = Json.format[SaAnnualEmployments]

  implicit val formatSaSelfEmploymentsIncome: Format[SaSelfEmploymentsIncome] = Json.format[SaSelfEmploymentsIncome]
  implicit val formatSaAnnualSelfEmployments: Format[SaAnnualSelfEmployments] = Json.format[SaAnnualSelfEmployments]

  implicit val formatSaTaxReturnSummary: Format[SaTaxReturnSummary] = Json.format[SaTaxReturnSummary]
  implicit val formatSaTaxReturnSummaries: Format[SaTaxReturnSummaries] = Json.format[SaTaxReturnSummaries]

  implicit val formatSaAnnualTrustIncome: Format[SaAnnualTrustIncome] = Json.format[SaAnnualTrustIncome]
  implicit val formatSaAnnualTrustIncomes: Format[SaAnnualTrustIncomes] = Json.format[SaAnnualTrustIncomes]

  implicit val formatSaAnnualForeignIncome: Format[SaAnnualForeignIncome] = Json.format[SaAnnualForeignIncome]
  implicit val formatSaAnnualForeignIncomes: Format[SaAnnualForeignIncomes] = Json.format[SaAnnualForeignIncomes]

  implicit val formatSaAnnualPartnershipIncome: Format[SaAnnualPartnershipIncome] =
    Json.format[SaAnnualPartnershipIncome]
  implicit val formatSaAnnualPartnershipIncomes: Format[SaAnnualPartnershipIncomes] =
    Json.format[SaAnnualPartnershipIncomes]

  implicit val formatSaAnnualInterestAndDividendIncome: Format[SaAnnualInterestAndDividendIncome] =
    Json.format[SaAnnualInterestAndDividendIncome]
  implicit val formatSaAnnualInterestAndDividendIncomes: Format[SaAnnualInterestAndDividendIncomes] =
    Json.format[SaAnnualInterestAndDividendIncomes]

  implicit val formatSaAnnualUkPropertiesIncome: Format[SaAnnualUkPropertiesIncome] =
    Json.format[SaAnnualUkPropertiesIncome]
  implicit val formatSaAnnualUkPropertiesIncomes: Format[SaAnnualUkPropertiesIncomes] =
    Json.format[SaAnnualUkPropertiesIncomes]

  implicit val formatSaAnnualPensionAndStateBenefitIncome: Format[SaAnnualPensionAndStateBenefitIncome] =
    Json.format[SaAnnualPensionAndStateBenefitIncome]
  implicit val formatSaAnnualPensionAndStateBenefitIncomes: Format[SaAnnualPensionAndStateBenefitIncomes] =
    Json.format[SaAnnualPensionAndStateBenefitIncomes]

  implicit val formatSaAnnualAdditionalInformation: Format[SaAnnualAdditionalInformation] =
    Json.format[SaAnnualAdditionalInformation]
  implicit val formatSaAnnualAdditionalInformations: Format[SaAnnualAdditionalInformations] =
    Json.format[SaAnnualAdditionalInformations]

  implicit val formatSaAnnualOtherIncome: Format[SaAnnualOtherIncome] = Json.format[SaAnnualOtherIncome]
  implicit val formatSaAnnualOtherIncomes: Format[SaAnnualOtherIncomes] = Json.format[SaAnnualOtherIncomes]
}

class InvalidEnumException(className: String, input: String)
    extends RuntimeException(s"Enumeration expected of type: '$className', but it does not contain '$input'")
