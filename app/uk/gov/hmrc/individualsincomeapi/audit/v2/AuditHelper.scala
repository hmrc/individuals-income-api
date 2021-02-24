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

import play.api.mvc.RequestHeader
import javax.inject.Inject
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.models.{ApiFailureResponseEventModel, ApiPayeResponseEventModel, ApiSaResponseEventModel, IfPayeApiResponseEventModel, IfSaApiResponseEventModel, ScopesAuditEventModel}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPayeEntry, IfSaEntry}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditHelper @Inject()(auditConnector: AuditConnector)
                           (implicit ec: ExecutionContext) {

  def auditApiResponse[T](correlationId: String,
                          matchId: String,
                          scopes: String,
                          request: RequestHeader,
                          selfLink: String,
                          response: Option[Seq[JsValue]])
                         (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "ApiResponseEvent",
      ApiPayeResponseEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        correlationId = Some(correlationId),
        scopes,
        returnLinks = selfLink,
        response = response
      )
    )

  def auditSaApiResponse[T](correlationId: String,
                          matchId: String,
                          scopes: String,
                          request: RequestHeader,
                          selfLink: String,
                          response: Option[JsValue])
                         (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "ApiResponseEvent",
      ApiSaResponseEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        correlationId = Some(correlationId),
        scopes,
        returnLinks = selfLink,
        response = response
      )
    )

  def auditApiFailure(correlationId: Option[String],
                      matchId: String,
                      request: RequestHeader,
                      requestUrl: String,
                      msg: String)
                     (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "ApiFailureEvent",
      ApiFailureResponseEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        correlationId = correlationId,
        requestUrl,
        msg
      )
    )

  def auditIfPayeApiResponse(correlationId: String,
                         matchId: String,
                         request: RequestHeader,
                         requestUrl: String,
                         ifPaye: Seq[IfPayeEntry])
                        (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "IfApiResponseEvent",
      IfPayeApiResponseEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        correlationId = correlationId,
        requestUrl = requestUrl,
        ifPaye = ifPaye
      )
    )

  def auditIfSaApiResponse(correlationId: String,
                           matchId: String,
                           request: RequestHeader,
                           requestUrl: String,
                           ifSa: Seq[IfSaEntry])
                          (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "IfApiResponseEvent",
      IfSaApiResponseEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        correlationId = correlationId,
        requestUrl = requestUrl,
        ifSa = ifSa
      )
    )

  def auditIfApiFailure(correlationId: String,
                        matchId: String,
                        request: RequestHeader,
                        requestUrl: String,
                        msg: String)
                       (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "IfApiFailureEvent",
      ApiFailureResponseEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        correlationId = Some(correlationId),
        requestUrl,
        msg
      )
    )

  def auditAuthScopes(matchId: String,
                      scopes:  String,
                      request: RequestHeader)
                     (implicit hc: HeaderCarrier) =
    auditConnector.sendExplicitAudit(
      "AuthScopesAuditEvent",
      ScopesAuditEventModel(
        ipAddress = hc.forwarded.map(_.value).getOrElse("-"),
        authorisation = hc.authorization.map(_.value).getOrElse("-"),
        deviceId = hc.deviceID.getOrElse("-"),
        input = s"Request to ${request.path}",
        method = request.method.toUpperCase,
        userAgent = request.headers.get("User-Agent").getOrElse("-"),
        apiVersion = "2.0",
        matchId = matchId,
        scopes
      )
    )
}
