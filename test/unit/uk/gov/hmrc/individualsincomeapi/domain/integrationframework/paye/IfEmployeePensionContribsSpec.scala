/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfEmployeePensionContribs
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IfEmployeePensionContribsSpec extends AnyWordSpec with Matchers {

  val validEmployeePensionContribs = IfEmployeePensionContribs(
    Some(169731.51),
    Some(173987.07),
    Some(822317.49),
    Some(818841.65)
  )

  val invalidEmployeePensionContribs = IfEmployeePensionContribs(
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1)
  )

  "IfEmployeePensionContribs" should {
    "WriteToJson" in {
      val expectedJson = Json.parse(
        """
          |{
          |    "paidYTD": 169731.51,
          |    "notPaidYTD": 173987.07,
          |    "paid": 822317.49,
          |    "notPaid": 818841.65
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validEmployeePensionContribs)

      result shouldBe expectedJson
    }

    "Validate successfully when given valid IfEmployeePensionContribs" in {
      val result = Json.toJson(validEmployeePensionContribs).validate[IfEmployeePensionContribs]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when given invalid IfEmployeePensionContribs" in {
      val result = Json.toJson(invalidEmployeePensionContribs).validate[IfEmployeePensionContribs]
      result.isError shouldBe true
    }
  }
}
