/*
 * Copyright 2020 HM Revenue & Customs
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
  implicit val payrollJsonFormat = Json.format[SaFootprint]

  def apply(ifSaEntry: Seq[IfSaEntry]): SaFootprint =
    SaFootprint(
      toSaFootprintRegistrations(ifSaEntry),
      toSaFootprintTaxReturn(ifSaEntry)
    )

  private def toSaFootprintRegistrations(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entryList =>
        entryList.returnList.map { rList =>
          rList.map { entry =>
            SaFootprintRegistration(entry.caseStartDate)
          }
        }
      }
      .flatten
      .sortBy(_.registrationDate)

  private def toSaFootprintSubmissions(entry: IfSaEntry) =
    entry.returnList.map { rList =>
      rList
        .map { entry =>
          SaFootprintSubmission(entry.receivedDate)
        }
        .sortBy(_.receivedDate)
    }

  private def toSaFootprintTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { e =>
        e.taxYear.map { ty =>
          SaFootprintTaxReturn(
            Some(TaxYear.fromEndYear(ty.toInt).formattedTaxYear),
            toSaFootprintSubmissions(e)
          )
        }
      }
      .sortBy(_.taxYear)
}
