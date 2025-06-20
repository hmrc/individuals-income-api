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

package uk.gov.hmrc.individualsincomeapi.cache

import play.api.libs.functional.syntax.*
import play.api.libs.json.{Format, JsPath, Json, Reads}
import java.time.LocalDateTime

case class ModifiedDetails(createdAt: LocalDateTime, lastUpdated: LocalDateTime)

object ModifiedDetails {
  implicit val format: Format[ModifiedDetails] = Format(
    (
      (JsPath \ "createdAt").read[LocalDateTime] and
        (JsPath \ "lastUpdated").read[LocalDateTime]
    )(ModifiedDetails.apply _),
    Json.writes[ModifiedDetails]
  )
}
