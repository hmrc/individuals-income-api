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

package uk.gov.hmrc.individualsincomeapi.audit.v2.events

import java.util.UUID

import javax.inject.Inject
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.HttpExtendedAuditEvent
import uk.gov.hmrc.individualsincomeapi.audit.v2.models.ApiResponseEventModel
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

case class ApiResponseEvent @Inject()(httpAuditEvent: HttpExtendedAuditEvent) {

  import httpAuditEvent.extendedDataEvent

  def apply(
    auditType: String,
    correlationId: String,
    scopes: String,
    matchId: UUID,
    request: RequestHeader,
    response: String)(
    implicit hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
  ): ExtendedDataEvent =
    extendedDataEvent(
      auditType,
      "APIResponseEvent",
      request,
      ApiResponseEventModel("2.0", matchId, correlationId, scopes, response).asInstanceOf[JsObject]
    )
}
