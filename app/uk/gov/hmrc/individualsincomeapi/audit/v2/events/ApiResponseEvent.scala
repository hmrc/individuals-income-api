package uk.gov.hmrc.individualsincomeapi.audit.v2.events

import java.util.UUID

import javax.inject.Inject
import play.api.libs.json.{Json, Writes}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, Request}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

case class ApiResponseEvent @Inject() (httpAuditEvent: HttpAuditEvent) {

  import httpAuditEvent.dataEvent

  def apply(auditType: String,
            correlationId: String,
            matchId: UUID,
            request: RequestHeader,
            response: String)
           (hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers),
            reqW: Writes[Request]): DataEvent =
    dataEvent(
      auditType,
      "APIResponseEvent",
      request,
      Map(
        "ApiVersion:"  -> "2.0",
        "matchId"       -> matchId.toString,
        "correlationId" -> correlationId.toString,
        "request"       -> Json.toJson(request).toString,
        "response"      -> response
      )
    )
}
