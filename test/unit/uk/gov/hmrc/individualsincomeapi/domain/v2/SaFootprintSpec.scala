/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.individualsincomeapi.domain.v2.SaFootprint
import utils.IncomeSaHelpers

class SaFootprintSpec extends WordSpec with Matchers with IncomeSaHelpers {

  val ifSa = Seq(createValidSaTaxYearEntry())

  "SaFootprint" should {
    "Write to Json for verbose data" in {
      val expectedJson = Json.parse("""{
                                      |  "registrations": [
                                      |    {
                                      |      "registrationDate": "2020-01-01"
                                      |    }
                                      |  ],
                                      |  "taxReturns": [
                                      |    {
                                      |      "taxYear": "2019-20",
                                      |      "submissions": [
                                      |        {
                                      |          "receivedDate": "2020-01-01"
                                      |        }
                                      |      ]
                                      |    }
                                      |  ]
                                      |}""".stripMargin)

      val result = Json.toJson(SaFootprint.transform(ifSa))

      result shouldBe expectedJson
    }

    "Write to Json for no root data" in {
      val expectedJson = Json.parse("""{
                                      |  "registrations": [],
                                      |  "taxReturns": []
                                      |}""".stripMargin)

      val result = Json.toJson(SaFootprint.transform(Seq()))

      result shouldBe expectedJson
    }

  }
}
