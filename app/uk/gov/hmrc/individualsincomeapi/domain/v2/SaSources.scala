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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfAdditionalFields, IfAddress, IfSaEntry}

case class SaSources(taxReturns: Seq[SaSourcesTaxReturn])

object SaSources {

  implicit val saForeignIncomesJsonFormat = Json.format[SaSources]

  def transform(ifSaEntry: Seq[IfSaEntry]) =
    SaSources(TransformSaSourcesTaxReturn(ifSaEntry))

  private def TransformSaSourceAddress(maybeAddress: Option[IfAddress]) =
    maybeAddress.map { address =>
      SaSourceAddress(
        address.line1,
        address.line2,
        address.line3,
        address.line4,
        address.line5,
        address.postcode
      )
    }

  private def default = SaSource(None, None, None)

  private def TransformSaSource(entry: IfSaEntry) =
    entry.returnList match {
      case Some(list) => {
        list.map { entry =>
          SaSource(
            entry.businessDescription,
            TransformSaSourceAddress(entry.address),
            entry.telephoneNumber
          )
        }
      }
      case _ => Seq(default)
    }

  private def TransformSaSourcesTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entry =>
        entry.taxYear.map { ty =>
          SaSourcesTaxReturn(
            TaxYear.fromEndYear(ty.toInt).formattedTaxYear,
            TransformSaSource(entry)
          )
        }
      }
      .sortBy(_.taxYear)

}
