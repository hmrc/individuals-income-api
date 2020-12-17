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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v2

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import uk.gov.hmrc.individualsincomeapi.services.v2.ScopesHelper

class IfQueriesSpec extends BaseSpec {

  feature("Query strings for 'paye' endpoint") {

    val helper: ScopesHelper = app.injector.instanceOf[ScopesHelper]

    val res1 =
      "paye(employeeNICs(inPayPeriod,inPayPeriod1,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
        "employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
        "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
        "paymentDate,payroll(id),statutoryPayYTD(adoption,maternity,paternity)," +
        "taxDeductedOrRefunded,taxablePay," +
        "totalEmployerNICs(InPayPeriod1,InPayPeriod2,InPayPeriod3,InPayPeriod4,ytd1,ytd2,ytd3,ytd4))"

    val res2 = "paye(employee(hasPartner)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4),payFrequency,paymentDate)"

    val res3 = "paye(employedPayeRef,grossEarningsForNICs" +
      "(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4),paymentDate,payroll(id))"

    val res4 =
      "paye(employeeNICs(inPayPeriod,inPayPeriod1,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
        "employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
        "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
        "monthlyPeriodNumber,paidHoursWorked,payFrequency,paymentDate," +
        "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
        "taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
        "totalEmployerNICs(InPayPeriod1,InPayPeriod2,InPayPeriod3,InPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
        "totalTaxToDate,weeklyPeriodNumber)"

    val res5 = "paye(dednsFromNetPay,grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "monthlyPeriodNumber,paidHoursWorked,payFrequency,paymentDate," +
      "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity),taxYear," +
      "taxablePayToDate,totalTaxToDate,weeklyPeriodNumber)"

    val res6 = "paye(employeeNICs(inPayPeriod,inPayPeriod1,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "monthlyPeriodNumber,paidHoursWorked,payFrequency,paymentDate," +
      "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
      "taxCode,taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
      "totalEmployerNICs(InPayPeriod1,InPayPeriod2,InPayPeriod3,InPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "totalTaxToDate,weeklyPeriodNumber)"

    val res7 = "paye(grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "paymentDate,payroll(id))"

    val res8 = "paye(dednsFromNetPay,employee(hasPartner)," +
      "employeeNICs(inPayPeriod,inPayPeriod1,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "monthlyPeriodNumber,payFrequency,paymentDate," +
      "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
      "taxCode,taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
      "totalEmployerNICs(InPayPeriod1,InPayPeriod2,InPayPeriod3,InPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "totalTaxToDate,weeklyPeriodNumber)"

    scenario("For read:individuals-income-nictsejo-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-nictsejo-c4"), List("incomePaye"))
      queryString shouldBe res1
    }

    scenario("For read:individuals-income-hmcts-c2") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c2"), List("incomePaye"))
      queryString shouldBe res2
    }

    scenario("For read:individuals-income-hmcts-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c3"), List("incomePaye"))
      queryString shouldBe res2
    }

    scenario("For read:individuals-income-hmcts-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c4"), List("incomePaye"))
      queryString shouldBe res3
    }

    scenario("For read:individuals-income-laa-c1") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c1"), List("incomePaye"))
      queryString shouldBe res4
    }

    scenario("For read:individuals-income-laa-c2") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c2"), List("incomePaye"))
      queryString shouldBe res5
    }

    scenario("For read:individuals-income-laa-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c3"), List("incomePaye"))
      queryString shouldBe res6
    }

    scenario("For read:individuals-income-laa-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c4"), List("incomePaye"))
      queryString shouldBe res7
    }

    scenario("For read:individuals-income-lsani-c1") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-lsani-c1"), List("incomePaye"))
      queryString shouldBe res8
    }

    scenario("For read:individuals-income-lsani-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-lsani-c3"), List("incomePaye"))
      queryString shouldBe res8
    }
  }
}
