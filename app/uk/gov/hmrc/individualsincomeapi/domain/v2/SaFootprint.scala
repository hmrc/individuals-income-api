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

package uk.gov.hmrc.individualsincomeapi.domain.v2

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry

case class SaFootprint(
  registrations: Seq[SaFootprintRegistration],
  taxReturns: Seq[SaFootprintTaxReturn]
)

object SaFootprint {
  implicit val saFootprintJsonFormat = Json.format[SaFootprint]

  def transform(ifSaEntry: Seq[IfSaEntry]): SaFootprint =
    SaFootprint(
      transformSaFootprintRegistrations(ifSaEntry),
      transformSaFootprintTaxReturn(ifSaEntry)
    )

  private def transformSaFootprintRegistrations(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entryList =>
        entryList.returnList.map { returns =>
          returns.flatMap { entry =>
            {
              entry.caseStartDate match {
                case Some(value) => Some(SaFootprintRegistration(Some(value)))
                case None        => None
              }
            }
          }
        }
      }
      .flatten
      .sortBy(x => x.registrationDate)

  private def default = SaFootprintSubmission(None)

  private def transformSaFootprintSubmissions(entry: IfSaEntry) = {
    entry.returnList match {
      case Some(list) =>
        list.map { entry =>
          SaFootprintSubmission(entry.receivedDate)
        }
      case _ => Seq(default)
    }
  }.sortBy(_.receivedDate)

  private def transformSaFootprintTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entry =>
        entry.taxYear.map { ty =>
          SaFootprintTaxReturn(
            TaxYear.fromEndYear(ty.toInt).formattedTaxYear,
            transformSaFootprintSubmissions(entry)
          )
        }
      }
      .sortBy(_.taxYear)
}
