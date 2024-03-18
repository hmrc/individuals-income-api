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

package uk.gov.hmrc.individualsincomeapi.domain.v2

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry

case class SaEmployments(taxReturns: Seq[SaEmploymentsTaxReturn])

object SaEmployments {

  implicit val saEmploymentsJsonFormat: OFormat[SaEmployments] = Json.format[SaEmployments]

  def transform(ifSaEntry: Seq[IfSaEntry]) =
    SaEmployments(transformSaEmploymentsTaxReturn(ifSaEntry))

  private def default = SaEmployment(0.0, None)

  private def transformSaEmployment(entry: IfSaEntry) =
    entry.returnList match {
      case Some(list) => {
        list.map { entry =>
          entry.income match {
            case Some(value) =>
              SaEmployment(value.allEmployments.getOrElse(0.0), entry.utr)
            case _ => default
          }
        }
      }
      case _ => Seq(default)
    }

  private def transformSaEmploymentsTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entry =>
        entry.taxYear.map { ty =>
          SaEmploymentsTaxReturn(
            TaxYear.fromEndYear(ty.toInt).formattedTaxYear,
            transformSaEmployment(entry)
          )
        }
      }
      .sortBy(_.taxYear)

}
