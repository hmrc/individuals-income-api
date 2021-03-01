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

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry

case class IfSaApiResponseEventModel(ipAddress: String,
                                     authorisation: String,
                                     deviceId: String,
                                     input: String,
                                     method: String,
                                     userAgent: String,
                                     apiVersion: String,
                                     matchId: String,
                                     correlationId: String,
                                     requestUrl: String,
                                     ifSa: Seq[IfSaEntry])

object IfSaApiResponseEventModel {
  implicit val formatIfSaApiResponseEventModel = Json.format[IfSaApiResponseEventModel]
}