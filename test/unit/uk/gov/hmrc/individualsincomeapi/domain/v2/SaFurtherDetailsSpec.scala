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

package unit.uk.gov.hmrc.individualsincomeapi.domain.v2

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.v2.SaFurtherDetails
import utils.IncomeSaHelpers

class SaFurtherDetailsSpec extends WordSpec with Matchers with IncomeSaHelpers {

  val ifSa = Seq(createValidSaTaxYearEntry())
  val ifSaNoData = Seq(createValidSaTaxYearEntryNoDataContainers())
  val ifSaNoValues = Seq(createValidSaTaxYearEntryNoValues())

  // TODO Q. Do we need the items in this json response to default to zero as other SA responses do?

  "SaFurtherDetails" should {
    "Write to Json for verbose data" in {
      val expectedJson = Json.parse("""{
                                      |  "taxReturns": [
                                      |    {
                                      |      "taxYear": "2019-20",
                                      |      "furtherDetails": [
                                      |        {
                                      |          "busStartDate": "2020-01-01",
                                      |          "busEndDate": "2020-01-30",
                                      |          "totalTaxPaid": 100.01,
                                      |          "totalNIC": 100.01,
                                      |          "turnover": 100.01,
                                      |          "otherBusIncome": 100.01,
                                      |          "tradingIncomeAllowance": 100.01,
                                      |          "deducts": {
                                      |            "totalBusExpenses": 200,
                                      |            "totalDisallowBusExp": 200
                                      |          }
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin)

      val result = Json.toJson(SaFurtherDetails.transform(ifSa))

      result shouldBe expectedJson
    }

    "Write to Json for no root data" in {
      val expectedJson = Json.parse("""{
                                      |  "taxReturns": []
                                      |}""".stripMargin)

      val result = Json.toJson(SaFurtherDetails.transform(Seq()))

      result shouldBe expectedJson
    }

    "Write to Json with defaults no data containers" in {

      val expectedJson = Json.parse("""{
                                      |  "taxReturns": [
                                      |    {
                                      |      "taxYear": "2019-20",
                                      |      "furtherDetails": [
                                      |        {
                                      |          "busStartDate": "2020-01-01",
                                      |          "busEndDate": "2020-01-30",
                                      |          "totalTaxPaid": 100.01,
                                      |          "totalNIC": 100.01,
                                      |          "turnover": 100.01,
                                      |          "otherBusIncome": 100.01,
                                      |          "tradingIncomeAllowance": 100.01
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin)

      val result = Json.toJson(SaFurtherDetails.transform(ifSaNoData))

      result shouldBe expectedJson
    }

    "Write to Json with defaults no vales" in {

      val expectedJson = Json.parse("""{
                                      |  "taxReturns": [
                                      |    {
                                      |      "taxYear": "2019-20",
                                      |      "furtherDetails": [
                                      |        {
                                      |          "busStartDate": "2020-01-01",
                                      |          "busEndDate": "2020-01-30"
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin)

      val result = Json.toJson(SaFurtherDetails.transform(ifSaNoValues))

      result shouldBe expectedJson
    }

  }
}
