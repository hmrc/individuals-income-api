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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import org.apache.pekko.stream.Materializer
import org.joda.time.{Interval, LocalDate}
import org.mockito.ArgumentMatchers.{any, eq => eqTo, _}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{times, verify}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.audit.v2.AuditHelper
import uk.gov.hmrc.individualsincomeapi.controllers.v2.IncomeController
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2.MatchedCitizen
import uk.gov.hmrc.individualsincomeapi.services.LiveCitizenMatchingService
import uk.gov.hmrc.individualsincomeapi.services.v2.{IncomeService, ScopesHelper, ScopesService}
import utils.{AuthHelper, IncomePayeHelpers, SpecBase}

import java.util.UUID
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

class IncomeControllerSpec extends SpecBase with AuthHelper with MockitoSugar with IncomePayeHelpers {
  implicit lazy val materializer: Materializer = fakeApplication().materializer

  trait Setup extends ScopesConfigHelper {

    val sampleCorrelationId = "188e9400-b636-4a3b-80ba-230a8c72b92a"
    val sampleCorrelationIdHeader = ("CorrelationId" -> sampleCorrelationId)

    val controllerComponent = fakeApplication().injector.instanceOf[ControllerComponents]
    val mockLiveIncomeService = mock[IncomeService]
    val mockLiveCitizenMatchingService = mock[LiveCitizenMatchingService]
    val mockAuditHelper = mock[AuditHelper]

    implicit lazy val ec = fakeApplication().injector.instanceOf[ExecutionContext]
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

    val incomeController =
      new IncomeController(mockLiveIncomeService, scopeService, mockAuthConnector, controllerComponent, mockAuditHelper)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockAuthConnector.authorise(eqTo(Enrolment("test-scope")), refEq(Retrievals.allEnrolments))(any(), any()))
      .willReturn(Future.successful(Enrolments(Set(Enrolment("test-scope")))))
  }

  def externalServices: Seq[String] = Seq("Stub")

  "Income controller income function" should {

    "return 200 when matching succeeds and service returns income" in new Setup {

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any(), any()))
        .willReturn(successful(ifPaye map IfPayeEntry.toIncome))

      val result =
        await(incomeController.income(matchId.toString, interval)(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

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
           |        "taxablePay":16533.95,
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

      verify(incomeController.auditHelper, times(1)).auditApiResponse(any(), any(), any(), any(), any(), any())(any())

    }

    "return 200 when matching succeeds and service returns no income" in new Setup {

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any(), any()))
        .willReturn(successful(Seq.empty))

      val result =
        await(incomeController.income(matchId.toString, interval)(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

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

      verify(incomeController.auditHelper, times(1)).auditApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 200 with correct self link response when toDate is not provided in the request" in new Setup {

      val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest("GET", s"/individuals/income/paye?matchId=$matchId&fromDate=$fromDateString")

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any(), any()))
        .willReturn(successful(Seq.empty))

      val result =
        await(incomeController.income(matchId.toString, interval)(fakeRequest.withHeaders(sampleCorrelationIdHeader)))

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

      verify(incomeController.auditHelper, times(1)).auditApiResponse(any(), any(), any(), any(), any(), any())(any())
    }

    "return 404 for an invalid matchId" in new Setup {

      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any(), any()))
        .willReturn(failed(new MatchNotFoundException()))

      val result =
        await(incomeController.income(matchId.toString, interval)(FakeRequest().withHeaders(sampleCorrelationIdHeader)))

      status(result) shouldBe NOT_FOUND

      verify(incomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())

    }

    "returns bad request with correct message when missing CorrelationId Header" in new Setup {
      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any(), any()))
        .willReturn(successful(Seq.empty))

      val result = await(incomeController.income(matchId.toString, interval)(FakeRequest()))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "CorrelationId is required"
          |}
          |""".stripMargin
      )

      verify(incomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }

    "return bad request with correct message when CorrelationId Header is malformed" in new Setup {
      given(mockLiveIncomeService.fetchIncomeByMatchId(eqTo(matchId), eqTo(interval), any())(any(), any()))
        .willReturn(successful(Seq.empty))

      val result = await(
        incomeController.income(matchId.toString, interval)(FakeRequest()
          .withHeaders("CorrelationId" -> "test")))

      status(result) shouldBe BAD_REQUEST
      jsonBodyOf(result) shouldBe Json.parse(
        """
          |{
          |  "code": "INVALID_REQUEST",
          |  "message": "Malformed CorrelationId"
          |}
          |""".stripMargin
      )

      verify(incomeController.auditHelper, times(1)).auditApiFailure(any(), any(), any(), any(), any())(any())
    }
  }
}
