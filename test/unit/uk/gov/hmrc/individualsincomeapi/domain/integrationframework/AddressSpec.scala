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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IncomeSa._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework._
import utils.UnitSpec

class AddressSpec extends UnitSpec {

  val validAddress = Address(Some("line1"), Some("line2"), Some("line3"), Some("line4"), None, Some("QW123QW"))
  val invalidAddress =
    Address(Some("line1"), Some("line2"), Some("line3"), Some("line4"), None, Some("1234567891011121314151617181920"))

  "Address" should {
    "Write to Json" in {
      val expectedJson = Json.parse("""
                                      |{
                                      |   "line1":"line1",
                                      |   "line2":"line2",
                                      |   "line3":"line3",
                                      |   "line4":"line4",
                                      |   "postcode":"QW123QW"
                                      |}
                                      |""".stripMargin)

      val result = Json.toJson(validAddress)

      result shouldBe expectedJson
    }

    "Validate successfully when address is valid" in {
      val result = Json.toJson(validAddress).validate[Address]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when address is invalid" in {
      val result = Json.toJson(invalidAddress).validate[Address]
      result.isError shouldBe true
    }
  }
}
