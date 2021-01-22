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

package uk.gov.hmrc.individualsincomeapi.audit.v2

import java.util.UUID

import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.events.{ApiResponseEvent, IfApiFailureEvent, IfApiResponseEvent}
import uk.gov.hmrc.individualsincomeapi.audit.v2.models.{ApiIfAuditRequest, ApiIfFailureAuditRequest}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

case class AuditHelper @Inject()(auditConnector: AuditConnector, httpExtendedAuditEvent: HttpExtendedAuditEvent)(
  implicit ec: ExecutionContext) {

  def auditApiResponse(
    correlationId: String,
    scopes: Option[String],
    matchId: Option[String],
    request: RequestHeader,
    response: JsValue)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      new ApiResponseEvent(
        httpExtendedAuditEvent
      ).apply(
        correlationId,
        scopes,
        matchId,
        request,
        None,
        response.toString
      )
    )

  def auditIfApiResponse(apiIfAuditRequest: ApiIfAuditRequest)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      new IfApiResponseEvent(
        httpExtendedAuditEvent
      ).apply(
        apiIfAuditRequest.correlationId,
        apiIfAuditRequest.scopes,
        apiIfAuditRequest.matchId,
        apiIfAuditRequest.request,
        Some(apiIfAuditRequest.requestUrl),
        apiIfAuditRequest.response.toString
      )
    )

  def auditIfApiFailure(apiIfFailedAuditRequest: ApiIfFailureAuditRequest, msg: String)(implicit hc: HeaderCarrier) =
    auditConnector.sendExtendedEvent(
      new IfApiFailureEvent(
        httpExtendedAuditEvent
      ).apply(
        apiIfFailedAuditRequest.correlationId,
        apiIfFailedAuditRequest.scopes,
        apiIfFailedAuditRequest.matchId,
        apiIfFailedAuditRequest.request,
        Some(apiIfFailedAuditRequest.requestUrl),
        msg
      )
    )
}
