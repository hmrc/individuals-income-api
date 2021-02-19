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

package uk.gov.hmrc.individualsincomeapi.audit.v2.models

case class ApiResponseEventModel[T](ipAddress: String,
                                    authorisation: String,
                                    deviceId: String,
                                    input: String,
                                    method: String,
                                    userAgent: String,
                                    apiVersion: String,
                                    matchId: String,
                                    correlationId: Option[String],
                                    scopes: String,
                                    returnLinks: String,
                                    response: Option[Seq[T]])

object ApiResponseEventModel {

  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._

  implicit def formatPayeResponseEventModel[T](implicit formatT: Format[T]): Format[ApiResponseEventModel[T]] =
    (
      (JsPath \ "ipAddress").format[String] and
        (JsPath \ "authorisation").format[String] and
        (JsPath \ "deviceId").format[String] and
        (JsPath \ "input").format[String] and
        (JsPath \ "method").format[String] and
        (JsPath \ "userAgent").format[String] and
        (JsPath \ "apiVersion").format[String] and
        (JsPath \ "matchId").format[String] and
        (JsPath \ "correlationId").formatNullable[String] and
        (JsPath \ "scopes").format[String] and
        (JsPath \ "returnLinks").format[String] and
        (JsPath \ "response").formatNullable[Seq[T]]
      )(ApiResponseEventModel.apply, unlift(ApiResponseEventModel.unapply))
}
