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

package unit.uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye

import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfStudentLoan
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IfStudentLoanSpec extends AnyWordSpec with Matchers {
  val validStudentLoan = IfStudentLoan(Some("01"), Some(100), Some(100))
  val invalidStudentLoan = IfStudentLoan(Some("NotValid"), Some(99999 + 1), Some(99999 + 1))

  "Student Loan" should {
    "WriteToJson" in {
      val expectedJson = Json.parse(
        """
          |{
          |  "planType" : "01",
          |  "repaymentsInPayPeriod" : 100,
          |  "repaymentsYTD" : 100
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validStudentLoan)

      result shouldBe (expectedJson)
    }

    "Validate successfully when given a valid Student Loan" in {
      val result = Json.toJson(validStudentLoan).validate[IfStudentLoan]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when given an invalid Student Loan" in {
      val result = Json.toJson(invalidStudentLoan).validate[IfStudentLoan]
      result.isError shouldBe true
    }
  }
}
