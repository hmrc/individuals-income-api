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

package unit.uk.gov.hmrc.individualsincomeapi.domain.integrationframework

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.Benefits
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IncomePaye._
import utils.UnitSpec

class BenefitsSpec extends UnitSpec {

  val validBenefits = Benefits(Some(506328.1), Some(246594.83))
  val invalidBenefits = Benefits(Some(9999999999.99 + 1), Some(9999999999.99 + 1))

  "Benefits" should {
    "Write to Json" in {
      val expectedJson = Json.parse(
        """
          |{
          |    "taxedViaPayroll": 506328.1,
          |    "taxedViaPayrollYTD": 246594.83
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validBenefits)

      result shouldBe expectedJson
    }
  }

  "Validate successfully when given valid Benefits" in {
    val result = Json.toJson(validBenefits).validate[Benefits]
    result.isSuccess shouldBe true
  }

  "Validate unsuccessfully when given invalid Benefits" in {
    val result = Json.toJson(invalidBenefits).validate[Benefits]
    result.isError shouldBe true
  }
}
