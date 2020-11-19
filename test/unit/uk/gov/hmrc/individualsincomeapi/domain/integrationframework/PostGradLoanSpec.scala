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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.PostGradLoan
import utils.UnitSpec
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IncomePaye._

class PostGradLoanSpec extends UnitSpec {

  val validPostGradLoan = PostGradLoan(Some(1588498.34), Some(2217757.33))
  val invalidPostGradLoan = PostGradLoan(Some(9999999999.99 + 1), Some(9999999999.99 + 1))

  "PostGradLoan" should {
    "Write to json" in {
      val expectedJson = Json.parse(
        """
          |{
          |  "repaymentsInPayPeriod": 1588498.34,
          |  "repaymentsYTD": 2217757.33
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validPostGradLoan)

      result shouldBe expectedJson
    }

    "Validate successfully with valid PostGradLoan" in {
      val result = Json.toJson(validPostGradLoan).validate[PostGradLoan]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully with invalid PostGradLoan" in {
      val result = Json.toJson(invalidPostGradLoan).validate[PostGradLoan]
      result.isError shouldBe true
    }
  }
}
