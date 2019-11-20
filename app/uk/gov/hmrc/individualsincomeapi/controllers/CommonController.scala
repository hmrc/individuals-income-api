/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.controllers

import org.joda.time.DateTime
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.{HeaderCarrier, TooManyRequestException}
import uk.gov.hmrc.individualsincomeapi.domain.{ErrorInvalidRequest, ErrorNotFound, ErrorTooManyRequests, ErrorUnauthorized, MatchNotFoundException}
import uk.gov.hmrc.individualsincomeapi.controllers.Environment.SANDBOX
import uk.gov.hmrc.individualsincomeapi.util.Dates._
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait CommonController extends BaseController {

  private def getQueryParam[T](name: String)(implicit request: Request[T]) = request.queryString.get(name).flatMap(_.headOption)

  private[controllers] def urlWithInterval[T](url: String, from: DateTime)(implicit request: Request[T]) = {
    val urlWithFromDate = s"$url&fromDate=${toFormattedLocalDate(from)}"
    getQueryParam("toDate").map(x => s"$urlWithFromDate&toDate=$x").getOrElse(urlWithFromDate)
  }

  private[controllers] def urlWithTaxYearInterval[T](url: String)(implicit request: Request[T]) = {
    (getQueryParam("fromTaxYear"), getQueryParam("toTaxYear")) match {
      case (Some(fromTaxYear), Some(toTaxYear)) => s"$url&fromTaxYear=$fromTaxYear&toTaxYear=$toTaxYear"
      case (Some(fromTaxYear), None) => s"$url&fromTaxYear=$fromTaxYear"
      case _ => url
    }
  }

  private[controllers] def recovery: PartialFunction[Throwable, Result] = {
    case _: MatchNotFoundException   => ErrorNotFound.toHttpResponse
    case e: AuthorisationException   => ErrorUnauthorized(e.getMessage).toHttpResponse
    case _: TooManyRequestException  => ErrorTooManyRequests.toHttpResponse
    case e: IllegalArgumentException => ErrorInvalidRequest(e.getMessage).toHttpResponse
  }
}

trait PrivilegedAuthentication extends AuthorisedFunctions {

  val environment: String

  def requiresPrivilegedAuthentication(scope: String)(body: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    if (environment == SANDBOX) body
    else authorised(Enrolment(scope))(body)
  }
}

object Environment {
  val SANDBOX = "SANDBOX"
  val PRODUCTION = "PRODUCTION"
}
