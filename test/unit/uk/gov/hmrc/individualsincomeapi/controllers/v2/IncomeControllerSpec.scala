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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import java.util.UUID

import akka.stream.Materializer
import org.joda.time.{Interval, LocalDate}
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito.given
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.domain.{EmpRef, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.controllers.v2.{LiveIncomeController, LiveRootController, SandboxIncomeController, SandboxRootController}
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.services.{LiveCitizenMatchingService, SandboxCitizenMatchingService}
import uk.gov.hmrc.individualsincomeapi.services.v2.{LiveIncomeService, SandboxIncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, IncomePayeHelpers, SpecBase}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.verifyNoInteractions
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2.MatchedCitizen

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.{failed, successful}

class IncomeControllerSpec extends SpecBase with AuthHelper with MockitoSugar with IncomePayeHelpers {
  implicit lazy val materializer: Materializer = fakeApplication.materializer

  trait Setup extends ScopesConfigHelper {

    val controllerComponent = fakeApplication.injector.instanceOf[ControllerComponents]
    val mockSandboxIncomeService = mock[SandboxIncomeService]
    val mockLiveIncomeService = mock[LiveIncomeService]
    val mockSandboxCitizenMatchingService = mock[SandboxCitizenMatchingService]
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]

    implicit lazy val ec = fakeApplication.injector.instanceOf[ExecutionContext]
    lazy val scopeService: ScopesService = new ScopesService(mockScopesConfig)
    lazy val scopesHelper: ScopesHelper = new ScopesHelper(scopeService)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val matchId = UUID.randomUUID()
    val nino = Nino("NA000799C")
    val matchedCitizen = MatchedCitizen(matchId, nino)

    val fromDateString = "2017-03-02"
    val toDateString = "2017-05-31"

    val interval = new Interval(
      new LocalDate(fromDateString).toDateTimeAtStartOfDay,
      new LocalDate(toDateString).toDateTimeAtStartOfDay
    )

    val ifPaye = Seq(createValidPayeEntry())

    val sandboxIncomeController =
      new SandboxIncomeController(mockSandboxIncomeService, scopeService, mockAuthConnector, controllerComponent)

    val liveIncomeController =
      new LiveIncomeController(mockLiveIncomeService, scopeService, mockAuthConnector, controllerComponent)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockAuthConnector.authorise(eqTo(Enrolment("test-scope")), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  def externalServices: Seq[String] = Seq("Stub")

  "Income controller income function" should {

    "return 200 when matching succeeds and service returns income" in new Setup {

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any()))
        .willReturn(successful(ifPaye map IfPayeEntry.toIncome))

      val result = await(liveIncomeController.income(matchId, interval)(FakeRequest()))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links":{
           |    "self":{
           |      "href":"/individuals/income/paye?matchId=$matchId&fromDate=2017-03-02"
           |    }
           |  },
           |  "paye":{
           |    "income":[
           |      {
           |        "employerPayeReference":"345/34678",
           |        "taxYear":"18-19",
           |        "employee": {
           |          "hasPartner": false
           |         },
           |         "payroll": {
           |           "id": "yxz8Lt5?/`/>6]5b+7%>o-y4~W5suW"
           |        },
           |        "payFrequency":"W4",
           |        "monthPayNumber": 3,
           |        "weekPayNumber": 2,
           |        "paymentDate":"2006-02-27",
           |        "paidHoursWorked":"36",
           |        "taxCode":"K971",
           |        "taxablePayToDate":19157.5,
           |        "totalTaxToDate":3095.89,
           |        "taxDeductedOrRefunded":159228.49,
           |        "dednsFromNetPay":198035.8,
           |        "employeePensionContribs":{
           |          "paidYTD":169731.51,
           |          "notPaidYTD":173987.07,
           |          "paid":822317.49,
           |          "notPaid":818841.65
           |        },
           |        "statutoryPayYTD":{
           |          "maternity":15797.45,
           |          "paternity":13170.69,
           |          "adoption":16193.76,
           |          "parentalBereavement":30846.56
           |        },
           |        "grossEarningsForNics":{
           |          "inPayPeriod1":169731.51,
           |          "inPayPeriod2":173987.07,
           |          "inPayPeriod3":822317.49,
           |          "inPayPeriod4":818841.65
           |        },
           |        "totalEmployerNics":{
           |          "inPayPeriod1":15797.45,
           |          "inPayPeriod2":13170.69,
           |          "inPayPeriod3":16193.76,
           |          "inPayPeriod4":30846.56,
           |          "ytd1":10633.5,
           |          "ytd2":15579.18,
           |          "ytd3":110849.27,
           |          "ytd4":162081.23
           |        },
           |        "employeeNics":{
           |          "inPayPeriod1":15797.45,
           |          "inPayPeriod2":13170.69,
           |          "inPayPeriod3":16193.76,
           |          "inPayPeriod4":30846.56,
           |          "ytd1":10633.5,
           |          "ytd2":15579.18,
           |          "ytd3":110849.27,
           |          "ytd4":162081.23
           |        }
           |      }
           |    ]
           |  }
           |}""".stripMargin
      )

    }

    "return 200 when matching succeeds and service returns no income" in new Setup {

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any()))
        .willReturn(successful(Seq.empty))

      val result = await(liveIncomeController.income(matchId, interval)(FakeRequest()))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links":{
           |    "self":{
           |      "href":"/individuals/income/paye?matchId=$matchId&fromDate=2017-03-02"
           |    }
           |  },
           |  "paye":{
           |    "income":[
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 200 with correct self link response when toDate is not provided in the request" in new Setup {

      val fakeRequest = FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString")

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any()))
        .willReturn(successful(Seq.empty))

      val result = await(liveIncomeController.income(matchId, interval)(FakeRequest()))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links":{
           |    "self":{
           |      "href":"/individuals/income/paye?matchId=$matchId&fromDate=2017-03-02"
           |    }
           |  },
           |  "paye":{
           |    "income":[
           |    ]
           |  }
           |}""".stripMargin
      )
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result = await(liveIncomeController.income(matchId, interval)(FakeRequest()))

      status(result) shouldBe NOT_FOUND

    }

    "not require bearer token authentication for Sandbox" in new Setup {

      given(mockSandboxIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any()))
        .willReturn(successful(Seq.empty))

      val result = await(sandboxIncomeController.income(matchId, interval)(FakeRequest()))

      status(result) shouldBe OK

      jsonBodyOf(result) shouldBe Json.parse(
        s"""{
           |  "_links":{
           |    "self":{
           |      "href":"/individuals/income/paye?matchId=$matchId&fromDate=2017-03-02"
           |    }
           |  },
           |  "paye":{
           |    "income":[
           |    ]
           |  }
           |}""".stripMargin
      )

      verifyNoInteractions(mockAuthConnector)
    }

  }
}
