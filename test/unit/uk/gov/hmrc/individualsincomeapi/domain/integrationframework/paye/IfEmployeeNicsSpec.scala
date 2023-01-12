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

package unit.uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfEmployeeNics
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IfEmployeeNicsSpec extends AnyWordSpec with Matchers {

  val validEmployeeNics = IfEmployeeNics(
    Some(15797.45),
    Some(13170.69),
    Some(16193.76),
    Some(30846.56),
    Some(10633.5),
    Some(15579.18),
    Some(110849.27),
    Some(162081.23)
  )

  val invalidEmployeeNics = IfEmployeeNics(
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1),
    Some(999999999.99 + 1)
  )

  "IfEmployeeNics" should {
    "Write to Json" in {
      val expectedJson = Json.parse(
        """
          |{
          |    "inPayPeriod1": 15797.45,
          |    "inPayPeriod2": 13170.69,
          |    "inPayPeriod3": 16193.76,
          |    "inPayPeriod4": 30846.56,
          |    "ytd1": 10633.5,
          |    "ytd2": 15579.18,
          |    "ytd3": 110849.27,
          |    "ytd4": 162081.23
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validEmployeeNics)

      result shouldBe expectedJson
    }

    "Validates successfully when passed a valid IfEmployeeNics" in {
      val result = Json.toJson(validEmployeeNics).validate[IfEmployeeNics]
      result.isSuccess shouldBe true
    }

    "Validates unsuccessfully when passed an invalid IfEmployeeNics" in {
      val result = Json.toJson(invalidEmployeeNics).validate[IfEmployeeNics]
      result.isError shouldBe true
    }
  }
}
