/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.config

import com.typesafe.config.Config
import uk.gov.hmrc.api.config.{ServiceLocatorConfig, ServiceLocatorRegistration}
import uk.gov.hmrc.api.connector.ServiceLocatorConnector
import uk.gov.hmrc.individualsincomeapi.domain.{ErrorInternalServer, ErrorInvalidRequest, ErrorUnauthorized}
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import net.ceedubs.ficus.Ficus._
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result}
import play.api.{Application, Configuration, Logger, Play}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils._
import uk.gov.hmrc.individualsincomeapi.domain.JsonFormatters.errorInvalidRequestFormat

import scala.concurrent.Future
import scala.concurrent.Future.successful
import scala.util.Try
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.filters.{ AuditFilter, LoggingFilter, MicroserviceFilterSupport }


object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceGlobal extends DefaultMicroserviceGlobal  with ServiceLocatorRegistration with ServiceLocatorConfig with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = None

  override lazy val registrationEnabled = Play.current.configuration.getBoolean("microservice.services.service-locator.enabled").getOrElse(false)

  override val slConnector = ServiceLocatorConnector(WSHttp)

  override implicit val hc = HeaderCarrier()

  private lazy val unversionedContexts = Play.current.configuration.getStringSeq("versioning.unversionedContexts").getOrElse(Seq.empty[String])

  override def onRequestReceived(originalRequest: RequestHeader) = {
    val requestContext = extractUriContext(originalRequest)
    if (unversionedContexts.contains(requestContext)) {
      super.onRequestReceived(originalRequest)
    } else {
      super.onRequestReceived(getVersionedRequest(originalRequest))
    }
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    ex match {
      case _: AuthorisationException => successful(ErrorUnauthorized.toHttpResponse)
      case _ =>
        Logger.error("An unexpected error occured", ex)
        successful(ErrorInternalServer.toHttpResponse)
    }
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {

    val maybeInvalidRequest = Try(Json.parse(error).as[ErrorInvalidRequest]).toOption

    maybeInvalidRequest match {
      case Some(errorResponse) => successful(errorResponse.toHttpResponse)
      case _ => successful(ErrorInvalidRequest("Invalid Request").toHttpResponse)
    }
  }

}
