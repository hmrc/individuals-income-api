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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IfPostGradLoan
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IfPayeEntry._

class IfPostGradLoanSpec extends WordSpec with Matchers {

  val validPostGradLoan = IfPostGradLoan(Some(99999), Some(22177))
  val invalidPostGradLoan = IfPostGradLoan(Some(99999 + 1), Some(-5))

  "IfPostGradLoan" should {
    "Write to json" in {
      val expectedJson = Json.parse(
        """
          |{
          |  "repaymentsInPayPeriod": 99999,
          |  "repaymentsYTD": 22177
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validPostGradLoan)

      result shouldBe expectedJson
    }

    "Validate successfully with valid IfPostGradLoan" in {
      val result = Json.toJson(validPostGradLoan).validate[IfPostGradLoan]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully with invalid IfPostGradLoan" in {
      val result = Json.toJson(invalidPostGradLoan).validate[IfPostGradLoan]
      result.isError shouldBe true
    }
  }
}
