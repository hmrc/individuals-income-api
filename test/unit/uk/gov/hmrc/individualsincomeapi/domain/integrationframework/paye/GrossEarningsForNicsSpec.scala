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

package unit.uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.GrossEarningsForNics
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IncomePaye._
import utils.UnitSpec

class GrossEarningsForNicsSpec extends UnitSpec {

  val validGrossEarningsForNics = GrossEarningsForNics(
    Some(995979.04),
    Some(606456.38),
    Some(797877.34),
    Some(166334.69)
  )

  val invalidGrossEarningsForNics = GrossEarningsForNics(
    Some(9999999999.99 + 1),
    Some(9999999999.99 + 1),
    Some(9999999999.99 + 1),
    Some(9999999999.99 + 1)
  )

  "GrossEarningsForNics" should {
    "Write to Json" in {
      val expectedJson = Json.parse(
        """
          |{
          |  "inPayPeriod1": 995979.04,
          |  "inPayPeriod2": 606456.38,
          |  "inPayPeriod3": 797877.34,
          |  "inPayPeriod4": 166334.69
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validGrossEarningsForNics)

      result shouldBe expectedJson
    }

    "Validates successfully when given a valid GrossEarningsForNics" in {
      val result = Json.toJson(validGrossEarningsForNics).validate[GrossEarningsForNics]
      result.isSuccess shouldBe true
    }

    "Validates unsuccessfully when given an invalid GrossEarningsForNics" in {
      val result = Json.toJson(invalidGrossEarningsForNics).validate[GrossEarningsForNics]
      result.isError shouldBe true
    }
  }
}
