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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.SaIncome
import utils.UnitSpec
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IncomeSa._

class SaIncomeSpec extends UnitSpec {

  val validSaIncome = SaIncome(
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0),
    Some(100.0)
  )

  val invalidSaIncome = SaIncome(
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001),
    Some(100.001)
  )

  "Sa Income" should {
    "Write To Json" in {
      val result = Json.toJson(validSaIncome)
      val expectedJson = Json.parse("""
                                      |{
                                      |  "selfAssessment" : 100,
                                      |  "allEmployments" : 100,
                                      |  "ukInterest" : 100,
                                      |  "foreignDivs" : 100,
                                      |  "ukDivsAndInterest" : 100,
                                      |  "partnerships" : 100,
                                      |  "pensions" : 100,
                                      |  "selfEmployment" : 100,
                                      |  "trusts" : 100,
                                      |  "ukProperty" : 100,
                                      |  "foreign" : 100,
                                      |  "lifePolicies" : 100,
                                      |  "shares" : 100,
                                      |  "other" : 100
                                      |}
                                      |""".stripMargin)

      result shouldBe expectedJson
    }

    "Validate successfully when SA Income is valid" in {
      val result = Json.toJson(validSaIncome).validate[SaIncome]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when SA Income is invalid" in {
      val result = Json.toJson(invalidSaIncome).validate[SaIncome]
      result.isError shouldBe true
    }
  }
}
