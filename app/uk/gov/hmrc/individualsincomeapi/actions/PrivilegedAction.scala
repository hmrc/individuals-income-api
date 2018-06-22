/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.actions

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.config.ServiceAuthConnector
import uk.gov.hmrc.individualsincomeapi.domain.{ErrorInvalidRequest, ErrorNotFound, MatchNotFoundException}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait PrivilegedAction {
  def apply(scope: String)(body: Request[AnyContent] => Future[Result]): Action[AnyContent]

  protected def recovery: PartialFunction[Throwable, Result] = {
    case _: MatchNotFoundException => ErrorNotFound.toHttpResponse
    case e: IllegalArgumentException => ErrorInvalidRequest(e.getMessage).toHttpResponse
  }
}

class SandboxPrivilegedAction @Inject()(implicit ec: ExecutionContext) extends PrivilegedAction {
  override def apply(scope: String)(body: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { request =>
    body(request) recover recovery
  }
}

class LivePrivilegedAction @Inject()(val authConnector: ServiceAuthConnector)
                                    (implicit ec: ExecutionContext) extends PrivilegedAction with AuthorisedFunctions {
  override def apply(scope: String)(body: Request[AnyContent] => Future[Result]): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    authorised(Enrolment(scope))(body(request)) recover recovery
  }
}
