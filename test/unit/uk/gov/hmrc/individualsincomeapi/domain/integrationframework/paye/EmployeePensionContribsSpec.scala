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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.EmployeePensionContribs
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IncomePaye._
import utils.UnitSpec

class EmployeePensionContribsSpec extends UnitSpec {

  val validEmployeePensionContribs = EmployeePensionContribs(
    Some(169731.51),
    Some(173987.07),
    Some(822317.49),
    Some(818841.65)
  )

  val invalidEmployeePensionContribs = EmployeePensionContribs(
    Some(9999999999.99 + 1),
    Some(9999999999.99 + 1),
    Some(9999999999.99 + 1),
    Some(9999999999.99 + 1)
  )

  "EmployeePensionContribs" should {
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

    "Validate successfully when given valid EmployeePensionContribs" in {
      val result = Json.toJson(validEmployeePensionContribs).validate[EmployeePensionContribs]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when given invalid EmployeePensionContribs" in {
      val result = Json.toJson(invalidEmployeePensionContribs).validate[EmployeePensionContribs]
      result.isError shouldBe true
    }
  }
}
