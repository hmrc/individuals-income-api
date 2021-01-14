package uk.gov.hmrc.individualsincomeapi.audit.v2

import java.util.UUID
import javax.inject.Inject
import play.api.mvc.RequestHeader
import uk.gov.hmrc.individualsincomeapi.audit.v2.events.ApiResponseEvent
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent

import scala.concurrent.ExecutionContext

case class AuditHelper @Inject() (auditConnector: AuditConnector, httpAuditEvent: HttpAuditEvent)
                                 (implicit ec: ExecutionContext) {

  private[controllers] def auditResponse(endpoint: String,
                                         correlationId: String,
                                         matchId: UUID,
                                         request: RequestHeader,
                                         response: String) = {
    auditConnector.sendEvent(
      ApiResponseEvent(
        httpAuditEvent
      ).apply(
        s"GET$endpoint",
        correlationId,
        matchId,
        request,
        response
      )
    )
  }
}
