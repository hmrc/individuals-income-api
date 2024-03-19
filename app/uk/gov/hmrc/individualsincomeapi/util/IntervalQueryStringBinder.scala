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

package uk.gov.hmrc.individualsincomeapi.util

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.individualsincomeapi.domain.ValidationException
import uk.gov.hmrc.individualsincomeapi.util.Dates.{localDatePattern, toInterval}

import java.time.LocalDate

class IntervalQueryStringBinder extends QueryStringBindable[Interval] {

  val dateTimeFormatter = Dates.localDatePattern

  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Interval]] =
    (getParam(params, "fromDate"), getParam(params, "toDate", Some(LocalDate.now()))) match {
      case (Right(from), Right(to)) => Some(interval(from, to))
      case (_, Left(msg))           => Some(Left(msg))
      case (Left(msg), _)           => Some(Left(msg))
    }

  private def interval(fromDate: LocalDate, toDate: LocalDate): Either[String, Interval] =
    try {
      Right(toInterval(fromDate, toDate))
    } catch {
      case e: ValidationException => Left(e.getMessage)
      case _: Throwable           => Left("Invalid time period requested")
    }

  private def getParam(
    params: Map[String, Seq[String]],
    paramName: String,
    default: Option[LocalDate] = None): Either[String, LocalDate] =
    try {
      params.get(paramName).flatMap(_.headOption) match {
        case Some(date) => Right(LocalDate.parse(date, localDatePattern))
        case None       => default.map(Right(_)).getOrElse(Left(s"$paramName is required"))
      }
    } catch {
      case _: Throwable => Left(s"$paramName: invalid date format")
    }

  override def unbind(key: String, dateRange: Interval): String =
    s"fromDate=${dateTimeFormatter.format(dateRange.from.toLocalDate)}&toDate=${dateTimeFormatter.format(dateRange.to.toLocalDate)}"
}
