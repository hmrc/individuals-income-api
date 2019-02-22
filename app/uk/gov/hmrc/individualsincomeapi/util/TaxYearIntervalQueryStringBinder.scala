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

package uk.gov.hmrc.individualsincomeapi.util

import uk.gov.hmrc.individualsincomeapi.config.ConfigSupport
import uk.gov.hmrc.individualsincomeapi.domain.{TaxYear, TaxYearInterval, ValidationException}
import uk.gov.hmrc.individualsincomeapi.util.Dates.toTaxYearInterval
import uk.gov.hmrc.play.config.ServicesConfig

class TaxYearIntervalQueryStringBinder extends AbstractQueryStringBindable[TaxYearInterval] with ServicesConfig with ConfigSupport {

  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, TaxYearInterval]] = {
    (getParam(params, "fromTaxYear"), getParam(params, "toTaxYear", Some(TaxYear.current()))) match {
      case (Right(from), Right(to)) => Some(taxYearInterval(from, to))
      case (_, Left(msg)) => Some(Left(msg))
      case (Left(msg), _) => Some(Left(msg))
    }
  }

  private def taxYearInterval(fromTaxYear: TaxYear, toTaxYear: TaxYear): Either[String, TaxYearInterval] = try {
    Right(toTaxYearInterval(fromTaxYear, toTaxYear))
  } catch {
    case e: ValidationException => Left(errorResponse(e.getMessage))
  }

  private def getParam(params: Map[String, Seq[String]], paramName: String, default: Option[TaxYear] = None): Either[String, TaxYear] = {
    try {
      params.get(paramName).flatMap(_.headOption) match {
        case Some(taxYear) => Right(TaxYear(taxYear))
        case None => default.map(Right(_)).getOrElse(Left(errorResponse(s"$paramName is required")))
      }
    } catch {
      case _: Throwable => Left(errorResponse(s"$paramName: invalid tax year format"))
    }
  }

  override def unbind(key: String, taxYearInterval: TaxYearInterval): String = {
    s"fromTaxYear=${taxYearInterval.fromTaxYear.formattedTaxYear}&toTaxYear=${taxYearInterval.toTaxYear.formattedTaxYear}"
  }
}
