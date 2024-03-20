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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry

case class SaPartnerships(taxReturns: Seq[SaPartnershipsTaxReturn])

object SaPartnerships {

  implicit val saPartnershipsJsonFormat: Format[SaPartnerships] = Json.format[SaPartnerships]

  def transform(ifSaEntry: Seq[IfSaEntry]): SaPartnerships =
    SaPartnerships(transformSaPartnershipsTaxReturn(ifSaEntry))

  private def default = SaPartnership(0.0)

  private def transformSaPartnership(entry: IfSaEntry) =
    entry.returnList match {
      case Some(list) => {
        list.map { entry =>
          entry.income match {
            case Some(value) =>
              SaPartnership(value.partnerships.getOrElse(0.0))
            case _ => default
          }
        }
      }
      case _ => Seq(default)
    }

  private def transformSaPartnershipsTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entry =>
        entry.taxYear.map { ty =>
          SaPartnershipsTaxReturn(
            TaxYear.fromEndYear(ty.toInt).formattedTaxYear,
            transformSaPartnership(entry)
          )
        }
      }
      .sortBy(_.taxYear)

}
