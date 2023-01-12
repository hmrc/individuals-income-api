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

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.TaxYear
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfSaEntry, IfSaReturn}

case class SaFurtherDetails(taxReturns: Seq[SaFurtherDetailsTaxReturn])

object SaFurtherDetails {

  implicit val saFurtherDetailsJsonFormat = Json.format[SaFurtherDetails]

  def transform(ifSaEntry: Seq[IfSaEntry]) =
    SaFurtherDetails(transformSaFurtherDetailsTaxReturn(ifSaEntry))

  private def transformSaFurtherDetailDeducts(ifSaReturn: IfSaReturn) =
    ifSaReturn.deducts.map { d =>
      SaFurtherDetailDeducts(
        d.totalBusExpenses,
        d.totalDisallowBusExp
      )
    }

  private def default = SaFurtherDetail(None, None, None, None, None, None, None, None)

  private def transformSaFurtherDetail(entry: IfSaEntry) =
    entry.returnList match {
      case Some(list) =>
        list.map { entry =>
          SaFurtherDetail(
            entry.busStartDate,
            entry.busEndDate,
            entry.totalTaxPaid,
            entry.totalNIC,
            entry.turnover,
            entry.otherBusinessIncome,
            entry.tradingIncomeAllowance,
            transformSaFurtherDetailDeducts(entry)
          )
        }
      case _ => Seq(default)
    }

  private def transformSaFurtherDetailsTaxReturn(ifSaEntry: Seq[IfSaEntry]) =
    ifSaEntry
      .flatMap { entry =>
        entry.taxYear.map { ty =>
          SaFurtherDetailsTaxReturn(
            TaxYear.fromEndYear(ty.toInt).formattedTaxYear,
            transformSaFurtherDetail(entry)
          )
        }
      }
      .sortBy(_.taxYear)

}
