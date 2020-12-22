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

case class SaInterestAndDividends(taxReturns: Seq[SaInterestAndDividendsTaxReturn])

object SaInterestAndDividends {

  implicit val saInterestAndDividendsJsonFormat = Json.format[SaInterestAndDividends]

  def transform(ifSaEntry: Seq[IfSaEntry]): SaInterestAndDividends =
    SaInterestAndDividends(TransformSaInterestAndDividendsTaxReturn(ifSaEntry))

  private def TransformSaInterestAndDividend(entry: IfSaEntry) =
    entry.returnList.map { returns =>
      returns.flatMap { entry =>
        entry.income.map { maybeIncome =>
          SaInterestAndDividend(
            maybeIncome.ukInterest,
            maybeIncome.foreignDivs,
            maybeIncome.ukDivsAndInterest
          )
        }
      }
    }

  private def TransformSaInterestAndDividendsTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entry =>
        entry.taxYear.map { ty =>
          SaInterestAndDividendsTaxReturn(
            Some(TaxYear.fromEndYear(ty.toInt).formattedTaxYear),
            TransformSaInterestAndDividend(entry)
          )
        }
      }
      .sortBy(_.taxYear)

}
