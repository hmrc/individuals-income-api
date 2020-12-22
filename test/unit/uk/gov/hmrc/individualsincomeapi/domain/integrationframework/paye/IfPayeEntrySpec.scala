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
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfAdditionalFields, IfPayeEntry}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry._
import utils.IncomePayeHelpers

class IfPayeEntrySpec extends WordSpec with Matchers with IncomePayeHelpers {

  "IfPayeEntry" should {
    "Write to json" in {
      val expectedJson = Json.parse(
        """
          |{
          |  "taxCode":"K971",
          |  "paidHoursWorked":"36",
          |  "taxablePayToDate":19157.5,
          |  "totalTaxToDate":3095.89,
          |  "taxDeductedOrRefunded":159228.49,
          |  "grossEarningsForNICs":{
          |    "inPayPeriod1":169731.51,
          |    "inPayPeriod2":173987.07,
          |    "inPayPeriod3":822317.49,
          |    "inPayPeriod4":818841.65
          |  },
          |  "employerPayeRef":"345/34678",
          |  "paymentDate":"2006-02-27",
          |  "taxablePay":16533.95,
          |  "taxYear":"18-19",
          |  "monthlyPeriodNumber":"3",
          |  "weeklyPeriodNumber":"2",
          |  "payFrequency":"W4",
          |  "dednsFromNetPay":198035.8,
          |  "totalEmployerNICs":{
          |    "inPayPeriod1":15797.45,
          |    "inPayPeriod2":13170.69,
          |    "inPayPeriod3":16193.76,
          |    "inPayPeriod4":30846.56,
          |    "ytd1":10633.5,
          |    "ytd2":15579.18,
          |    "ytd3":110849.27,
          |    "ytd4":162081.23},
          |    "employeeNICs":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    },
          |    "employeePensionContribs":{
          |      "paidYTD":169731.51,
          |      "notPaidYTD":173987.07,
          |      "paid":822317.49,
          |      "notPaid":818841.65
          |    },
          |    "benefits":{
          |      "taxedViaPayroll":506328.1,
          |      "taxedViaPayrollYTD":246594.83
          |    },
          |    "statutoryPayYTD":{
          |      "maternity":15797.45,
          |      "paternity":13170.69,
          |      "adoption":16193.76,
          |      "parentalBereavement":30846.56
          |    },
          |    "studentLoan":{
          |      "planType":"02",
          |      "repaymentsInPayPeriod":88478,
          |      "repaymentsYTD":545
          |    },
          |    "postGradLoan":{
          |      "repaymentsInPayPeriod":15636,
          |      "repaymentsYTD":46849
          |    },
          |    "payroll":{
          |      "id":"yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"
          |    },
          |    "employee":{
          |      "hasPartner":false}
          |    }
          |""".stripMargin
      )

      val result = Json.toJson(createValidPayeEntry())

      result shouldBe expectedJson
    }

    "Validate successfully when given valid IfPayeEntry" in {
      val result = Json.toJson(createValidPayeEntry()).validate[IfPayeEntry]
      result.isSuccess shouldBe true
    }

    "Validate unsuccessfully when given invalid IfPayeEntry" in {
      val result = Json.toJson(createInvalidPayeEntry()).validate[IfPayeEntry]
      result.isError shouldBe true
    }

    "transform to income type correctly" in {
      val converted = Seq(createValidPayeEntry()) map IfPayeEntry.toIncome
      val result = Json.toJson(converted)

      val expectedJson = Json.parse(
        """
          |[{
          |    "employerPayeReference":"345/34678",
          |    "taxYear":"18-19",
          |    "employee":{
          |      "hasPartner":false
          |    },
          |    "payroll":{
          |      "id":"yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"
          |    },
          |    "payFrequency":"W4",
          |    "monthPayNumber": 3,
          |    "weekPayNumber": 2,
          |    "paymentDate":"2006-02-27",
          |    "paidHoursWorked":"36",
          |    "taxCode":"K971",
          |    "taxablePayToDate":19157.5,
          |    "totalTaxToDate":3095.89,
          |    "taxDeductedOrRefunded":159228.49,
          |    "dednsFromNetPay":198035.8,
          |    "employeePensionContribs":{
          |      "paidYTD":169731.51,
          |      "notPaidYTD":173987.07,
          |      "paid":822317.49,
          |      "notPaid":818841.65
          |    },"statutoryPayYTD":{
          |      "maternity":15797.45,
          |      "paternity":13170.69,
          |      "adoption":16193.76,
          |      "parentalBereavement":30846.56
          |    },
          |    "grossEarningsForNics":{
          |      "inPayPeriod1":169731.51,
          |      "inPayPeriod2":173987.07,
          |      "inPayPeriod3":822317.49,
          |      "inPayPeriod4":818841.65
          |    },
          |    "totalEmployerNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    },
          |    "employeeNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    }
          |  }
          |]
          |""".stripMargin
      )

      result shouldBe expectedJson
    }

    "transform to income type correctly no employee" in {

      val ifPaye = Seq(
        createValidPayeEntry().copy(
          additionalFields = Some(IfAdditionalFields(None, Some("yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW")))
        )
      )

      val converted = ifPaye map IfPayeEntry.toIncome

      val result = Json.toJson(converted)

      val expectedJson = Json.parse(
        """
          |[{
          |    "employerPayeReference":"345/34678",
          |    "taxYear":"18-19",
          |    "payroll":{
          |      "id":"yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"
          |    },
          |    "payFrequency":"W4",
          |    "monthPayNumber": 3,
          |    "weekPayNumber": 2,
          |    "paymentDate":"2006-02-27",
          |    "paidHoursWorked":"36",
          |    "taxCode":"K971",
          |    "taxablePayToDate":19157.5,
          |    "totalTaxToDate":3095.89,
          |    "taxDeductedOrRefunded":159228.49,
          |    "dednsFromNetPay":198035.8,
          |    "employeePensionContribs":{
          |      "paidYTD":169731.51,
          |      "notPaidYTD":173987.07,
          |      "paid":822317.49,
          |      "notPaid":818841.65
          |    },"statutoryPayYTD":{
          |      "maternity":15797.45,
          |      "paternity":13170.69,
          |      "adoption":16193.76,
          |      "parentalBereavement":30846.56
          |    },
          |    "grossEarningsForNics":{
          |      "inPayPeriod1":169731.51,
          |      "inPayPeriod2":173987.07,
          |      "inPayPeriod3":822317.49,
          |      "inPayPeriod4":818841.65
          |    },
          |    "totalEmployerNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    },
          |    "employeeNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    }
          |  }
          |]
          |""".stripMargin
      )

      result shouldBe expectedJson
    }

    "transform to income type correctly no payroll id" in {

      val ifPaye = Seq(
        createValidPayeEntry().copy(
          additionalFields = Some(IfAdditionalFields(Some(true), None))
        )
      )

      val converted = ifPaye map IfPayeEntry.toIncome

      val result = Json.toJson(converted)

      val expectedJson = Json.parse(
        """
          |[{
          |    "employerPayeReference":"345/34678",
          |    "taxYear":"18-19",
          |    "employee":{
          |      "hasPartner":true
          |    },
          |    "payFrequency":"W4",
          |    "monthPayNumber": 3,
          |    "weekPayNumber": 2,
          |    "paymentDate":"2006-02-27",
          |    "paidHoursWorked":"36",
          |    "taxCode":"K971",
          |    "taxablePayToDate":19157.5,
          |    "totalTaxToDate":3095.89,
          |    "taxDeductedOrRefunded":159228.49,
          |    "dednsFromNetPay":198035.8,
          |    "employeePensionContribs":{
          |      "paidYTD":169731.51,
          |      "notPaidYTD":173987.07,
          |      "paid":822317.49,
          |      "notPaid":818841.65
          |    },"statutoryPayYTD":{
          |      "maternity":15797.45,
          |      "paternity":13170.69,
          |      "adoption":16193.76,
          |      "parentalBereavement":30846.56
          |    },
          |    "grossEarningsForNics":{
          |      "inPayPeriod1":169731.51,
          |      "inPayPeriod2":173987.07,
          |      "inPayPeriod3":822317.49,
          |      "inPayPeriod4":818841.65
          |    },
          |    "totalEmployerNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    },
          |    "employeeNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    }
          |  }
          |]
          |""".stripMargin
      )

      result shouldBe expectedJson
    }

    "transform to income type correctly no payroll id or employee" in {

      val ifPaye = Seq(createValidPayeEntry().copy(additionalFields = None))

      val converted = ifPaye map IfPayeEntry.toIncome

      val result = Json.toJson(converted)

      val expectedJson = Json.parse(
        """
          |[{
          |    "employerPayeReference":"345/34678",
          |    "taxYear":"18-19",
          |    "payFrequency":"W4",
          |    "monthPayNumber": 3,
          |    "weekPayNumber": 2,
          |    "paymentDate":"2006-02-27",
          |    "paidHoursWorked":"36",
          |    "taxCode":"K971",
          |    "taxablePayToDate":19157.5,
          |    "totalTaxToDate":3095.89,
          |    "taxDeductedOrRefunded":159228.49,
          |    "dednsFromNetPay":198035.8,
          |    "employeePensionContribs":{
          |      "paidYTD":169731.51,
          |      "notPaidYTD":173987.07,
          |      "paid":822317.49,
          |      "notPaid":818841.65
          |    },"statutoryPayYTD":{
          |      "maternity":15797.45,
          |      "paternity":13170.69,
          |      "adoption":16193.76,
          |      "parentalBereavement":30846.56
          |    },
          |    "grossEarningsForNics":{
          |      "inPayPeriod1":169731.51,
          |      "inPayPeriod2":173987.07,
          |      "inPayPeriod3":822317.49,
          |      "inPayPeriod4":818841.65
          |    },
          |    "totalEmployerNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    },
          |    "employeeNics":{
          |      "inPayPeriod1":15797.45,
          |      "inPayPeriod2":13170.69,
          |      "inPayPeriod3":16193.76,
          |      "inPayPeriod4":30846.56,
          |      "ytd1":10633.5,
          |      "ytd2":15579.18,
          |      "ytd3":110849.27,
          |      "ytd4":162081.23
          |    }
          |  }
          |]
          |""".stripMargin
      )

      result shouldBe expectedJson
    }
  }
}
