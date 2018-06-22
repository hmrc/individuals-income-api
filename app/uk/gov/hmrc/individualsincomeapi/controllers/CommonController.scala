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

package uk.gov.hmrc.individualsincomeapi.controllers

import org.joda.time.DateTime
import play.api.mvc.Request
import uk.gov.hmrc.individualsincomeapi.util.Dates._
import uk.gov.hmrc.play.microservice.controller.BaseController

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
}
