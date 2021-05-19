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

package component.uk.gov.hmrc.individualsincomeapi.controllers.v2

import component.uk.gov.hmrc.individualsincomeapi.stubs.BaseSpec
import uk.gov.hmrc.individualsincomeapi.services.v2.ScopesHelper

class IfQueriesSpec extends BaseSpec {

  feature("Query strings for 'paye' endpoint") {

    val helper: ScopesHelper = app.injector.instanceOf[ScopesHelper]

    val res1 =
      "paye(employeeNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
        "employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
        "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
        "paymentDate,payroll(id),statutoryPayYTD(adoption,maternity,paternity)," +
        "taxDeductedOrRefunded,taxablePay," +
        "totalEmployerNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4))"

    val res2 = "paye(employee(hasPartner)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4),payFrequency,paymentDate)"

    val res3 = "paye(employedPayeRef,grossEarningsForNICs" +
      "(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4),paymentDate,payroll(id))"

    val res4 =
      "paye(employeeNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
        "employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
        "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
        "monthlyPeriodNumber,paidHoursWorked,payFrequency,paymentDate," +
        "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
        "taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
        "totalEmployerNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
        "totalTaxToDate,weeklyPeriodNumber)"

    val res5 = "paye(dednsFromNetPay,grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "monthlyPeriodNumber,paidHoursWorked,payFrequency,paymentDate," +
      "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity),taxYear," +
      "taxablePayToDate,totalTaxToDate,weeklyPeriodNumber)"

    val res6 = "paye(employeeNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "monthlyPeriodNumber,paidHoursWorked,payFrequency,paymentDate," +
      "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
      "taxCode,taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
      "totalEmployerNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "totalTaxToDate,weeklyPeriodNumber)"

    val res7 = "paye(grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "paymentDate,payroll(id))"

    val res8 = "paye(dednsFromNetPay,employee(hasPartner)," +
      "employeeNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "employeePensionContribs(notPaid,notPaidYTD,paid,paidYTD)," +
      "grossEarningsForNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4)," +
      "monthlyPeriodNumber,payFrequency,paymentDate," +
      "statutoryPayYTD(adoption,maternity,parentalBereavement,paternity)," +
      "taxCode,taxDeductedOrRefunded,taxYear,taxablePay,taxablePayToDate," +
      "totalEmployerNICs(inPayPeriod1,inPayPeriod2,inPayPeriod3,inPayPeriod4,ytd1,ytd2,ytd3,ytd4)," +
      "totalTaxToDate,weeklyPeriodNumber)"

    scenario("For read:individuals-income-nictsejo-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-nictsejo-c4"), List("paye"))
      queryString shouldBe res1
    }

    scenario("For read:individuals-income-hmcts-c2") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c2"), List("paye"))
      queryString shouldBe res2
    }

    scenario("For read:individuals-income-hmcts-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c3"), List("paye"))
      queryString shouldBe res2
    }

    scenario("For read:individuals-income-hmcts-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c4"), List("paye"))
      queryString shouldBe res3
    }

    scenario("For read:individuals-income-laa-c1") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c1"), List("paye"))
      queryString shouldBe res4
    }

    scenario("For read:individuals-income-laa-c2") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c2"), List("paye"))
      queryString shouldBe res5
    }

    scenario("For read:individuals-income-laa-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c3"), List("paye"))
      queryString shouldBe res6
    }

    scenario("For read:individuals-income-laa-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c4"), List("paye"))
      queryString shouldBe res7
    }

    scenario("For read:individuals-income-lsani-c1") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-lsani-c1"), List("paye"))
      queryString shouldBe res8
    }

    scenario("For read:individuals-income-lsani-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-lsani-c3"), List("paye"))
      queryString shouldBe res8
    }
  }

  feature("Query strings for 'sa' endpoint") {

    def endpoints =
      List(
        "sa",
        "summary",
        "trusts",
        "foreign",
        "partnerships",
        "interestsAndDividends",
        "pensionsAndStateBenefits",
        "ukProperties",
        "additionalInformation",
        "other",
        "source",
        "employments",
        "selfEmployments",
        "furtherDetails"
      )

    val helper: ScopesHelper = app.injector.instanceOf[ScopesHelper]

    val res1 = "sa(returnList(address(line1,line2,line3,line4,postcode),businessDescription," +
      "caseStartDate,income(allEmployments,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment," +
      "selfEmployment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty),receivedDate," +
      "telephoneNumber),taxYear)"

    val res2 =
      "sa(returnList(caseStartDate,income(foreign,foreignDivs,selfAssessment,selfEmployment," +
        "ukDivsAndInterest,ukInterest,ukProperty)),taxYear)"

    val res3 = "sa(returnList(caseStartDate,income(foreign,foreignDivs,selfAssessment,selfEmployment," +
      "ukDivsAndInterest,ukInterest,ukProperty)),taxYear)"

    val res4 = "sa(returnList(address(line1,line2,line3,line4,postcode),businessDescription," +
      "caseStartDate,telephoneNumber),taxYear)"

    val res5 = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate,busStartDate," +
      "caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp),income(allEmployments,foreign,foreignDivs," +
      "lifePolicies,other,partnerships,pensions,selfAssessment,shares,trusts,ukDivsAndInterest,ukInterest," +
      "ukProperty),otherBusIncome,totalNIC,totalTaxPaid,tradingIncomeAllowance,turnover),taxYear)"

    val res6 = "sa(returnList(busEndDate,busStartDate,caseStartDate,deducts(totalBusExpenses,totalDisallowBusExp)," +
      "income(allEmployments,foreign,foreignDivs,lifePolicies,other,partnerships,pensions,selfAssessment,shares," +
      "trusts,ukDivsAndInterest,ukInterest,ukProperty),otherBusIncome,tradingIncomeAllowance,turnover),taxYear)"

    val res7 = "sa(returnList(address(line1,line2,line3,line4,postcode),busEndDate,busStartDate," +
      "businessDescription,deducts(totalBusExpenses,totalDisallowBusExp),income(allEmployments,foreign,foreignDivs," +
      "lifePolicies,other,partnerships,pensions,selfAssessment,shares,trusts,ukDivsAndInterest,ukInterest,ukProperty)," +
      "otherBusIncome,tradingIncomeAllowance,turnover),taxYear)"

    val res8 = "sa(returnList(address(line1,line2,line3,line4,postcode),businessDescription,telephoneNumber),taxYear)"

    val res9 = "sa(returnList(busEndDate,busStartDate,deducts(totalBusExpenses),income(allEmployments,foreign,foreignDivs," +
      "lifePolicies,other,partnerships,pensions,selfAssessment,selfEmployment,shares,trusts,ukDivsAndInterest," +
      "ukInterest,ukProperty),receivedDate,totalNIC,totalTaxPaid),taxYear)"

    scenario("For read:individuals-income-nictsejo-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-nictsejo-c4"), endpoints)
      queryString shouldBe res1
    }

    scenario("For read:individuals-income-hmcts-c2") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c2"), endpoints)
      queryString shouldBe res2
    }

    scenario("For read:individuals-income-hmcts-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c3"), endpoints)
      queryString shouldBe res3
    }

    scenario("For read:individuals-income-hmcts-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-hmcts-c4"), endpoints)
      queryString shouldBe res4
    }

    scenario("For read:individuals-income-laa-c1") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c1"), endpoints)
      queryString shouldBe res5
    }

    scenario("For read:individuals-income-laa-c2") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c2"), endpoints)
      queryString shouldBe res6
    }

    scenario("For read:individuals-income-laa-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c3"), endpoints)
      queryString shouldBe res7
    }

    scenario("For read:individuals-income-laa-c4") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-laa-c4"), endpoints)
      queryString shouldBe res8
    }

    scenario("For read:individuals-income-lsani-c1") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-lsani-c1"), endpoints)
      queryString shouldBe res9
    }

    scenario("For read:individuals-income-lsani-c3") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-lsani-c3"), endpoints)
      queryString shouldBe res9
    }

    scenario("For read:individuals-income-ho-ecp-application") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-ho-ecp-application"), endpoints)
      queryString shouldBe "sa(returnList(income(allEmployments,other,selfAssessment,selfEmployment),receivedDate,utr),taxYear)"
    }

    scenario("For read:individuals-income-ho-ecp-compliance") {
      val queryString = helper.getQueryStringFor(Seq("read:individuals-income-ho-ecp-compliance"), endpoints)
      queryString shouldBe "sa(returnList(income(allEmployments,other,selfAssessment,selfEmployment),receivedDate,utr),taxYear)"
    }
  }
}
