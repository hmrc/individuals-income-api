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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.IncomePaye._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye._
import utils.UnitSpec

class PayeEntrySpec extends UnitSpec {

  val validPayeEntry = PayeEntry(
    Some("K971"),
    Some("36"),
    Some(19157.5),
    Some(3095.89),
    Some(159228.49),
    Some("345/34678"),
    Some("2006-02-27"),
    Some(16533.95),
    Some("18-19"),
    Some("3"),
    Some("2"),
    Some("W4"),
    Some(198035.8),
    Some(createValidEmployeeNics()),
    Some(createValidEmployeePensionContribs()),
    Some(createValidBenefits()),
    Some(39708.7),
    Some(createValidStudentLoan()),
    Some(createValidPostGradLoan())
  )

  val invalidPayeEntry = PayeEntry(
    Some("TEST"),
    Some("TEST"),
    Some(19157.5),
    Some(3095.89),
    Some(159228.49),
    Some("TEST"),
    Some("TEST"),
    Some(16533.95),
    Some("TEST"),
    Some("TEST"),
    Some("TEST"),
    Some("TEST"),
    Some(198035.8),
    Some(createValidEmployeeNics()),
    Some(createValidEmployeePensionContribs()),
    Some(createValidBenefits()),
    Some(39708.7),
    Some(createValidStudentLoan()),
    Some(createValidPostGradLoan())
  )

  "PayeEntry" should {
    "Write to json" in {
      val expectedJson = Json.parse(
        """
          |{
          |    "taxCode": "K971",
          |    "paidHoursWorked": "36",
          |    "taxablePayToDate": 19157.5,
          |    "totalTaxToDate": 3095.89,
          |    "taxDeductedOrRefunded": 159228.49,
          |    "employerPayeRef": "345/34678",
          |    "paymentDate": "2006-02-27",
          |    "taxablePay": 16533.95,
          |    "taxYear": "18-19",
          |    "monthlyPeriodNumber": "3",
          |    "weeklyPeriodNumber": "2",
          |    "payFrequency": "W4",
          |    "dednsFromNetPay": 198035.8,
          |    "employeeNICs": {
          |        "inPayPeriod1": 15797.45,
          |        "inPayPeriod2": 13170.69,
          |        "inPayPeriod3": 16193.76,
          |        "inPayPeriod4": 30846.56,
          |        "ytd1": 10633.5,
          |        "ytd2": 15579.18,
          |        "ytd3": 110849.27,
          |        "ytd4": 162081.23
          |    },
          |    "employeePensionContribs": {
          |        "paidYTD": 169731.51,
          |        "notPaidYTD": 173987.07,
          |        "paid": 822317.49,
          |        "notPaid": 818841.65
          |    },
          |    "benefits": {
          |        "taxedViaPayroll": 506328.1,
          |        "taxedViaPayrollYTD": 246594.83
          |    },
          |    "statutoryPayYTD": {
          |        "parentalBereavement": 39708.7
          |    },
          |    "studentLoan": {
          |        "planType": "02",
          |        "repaymentsInPayPeriod": 88478.16,
          |        "repaymentsYTD": 545.52
          |    },
          |    "postGradLoan": {
          |        "repaymentsInPayPeriod": 15636.22,
          |        "repaymentsYTD": 46849.26
          |    }
          |}
          |""".stripMargin
      )

      val result = Json.toJson(validPayeEntry)

      result shouldBe expectedJson
    }

    "Validate successfully when given valid PayeEntry" in {
      val result = Json.toJson(validPayeEntry).validate[PayeEntry]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when given invalid PayeEntry" in {
      val result = Json.toJson(invalidPayeEntry).validate[PayeEntry]
      result.isError shouldBe true
    }
  }

  private def createValidEmployeeNics() =
    EmployeeNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  private def createValidEmployeePensionContribs() =
    EmployeePensionContribs(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))

  private def createValidBenefits() = Benefits(Some(506328.1), Some(246594.83))

  private def createValidStudentLoan() = StudentLoan(Some("02"), Some(88478.16), Some(545.52))

  private def createValidPostGradLoan() = PostGradLoan(Some(15636.22), Some(46849.26))
}
