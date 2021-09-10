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

package unit.uk.gov.hmrc.individualsincomeapi.services.v1

import java.util.UUID

import org.joda.time.LocalDate
import org.joda.time.LocalDate.parse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.BDDMockito.given
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Format
import uk.gov.hmrc.domain.{EmpRef, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData._
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesEmployment, DesEmployments, DesPayment}
import uk.gov.hmrc.individualsincomeapi.domain.v1.{MatchedCitizen, Payment}
import uk.gov.hmrc.individualsincomeapi.services.v1.{CacheId, LiveIncomeService, PayeIncomeCache, SandboxIncomeService}
import unit.uk.gov.hmrc.individualsincomeapi.util.TestDates
import utils.SpecBase

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class IncomeServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with TestDates {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    // can't mock function with by-value arguments
    val stubCache = new PayeIncomeCache(null, null) {
      override def get[T: Format](cacheId: CacheId, fallbackFunction: => Future[T]) =
        fallbackFunction
    }

    val mockMatchingConnector = mock[IndividualsMatchingApiConnector]
    val mockDesConnector = mock[DesConnector]
    val liveIncomeService = new LiveIncomeService(mockMatchingConnector, mockDesConnector, 1, stubCache)
    val sandboxIncomeService = new SandboxIncomeService()
  }

  "liveIncomeService fetch income by matchId function" should {
    val matchedCitizen = MatchedCitizen(UUID.randomUUID(), Nino("AA100009B"))
    val interval = toInterval("2016-01-12", "2016-03-22")

    "return the employment's income" in new Setup {
      val desEmployments = Seq(DesEmployment(Seq(DesPayment(parse("2016-02-28"), 10.50))))

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockDesConnector.fetchEmployments(eqTo(matchedCitizen.nino), eqTo(interval))(any(), any()))
        .willReturn(successful(desEmployments))

      val result = await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval)(hc))

      result shouldBe List(Payment(10.5, parse("2016-02-28")))
    }

    "Sort the payments by payment date descending" in new Setup {
      val desEmployments = Seq(
        DesEmployment(
          Seq(
            DesPayment(parse("2016-02-28"), 10.50),
            DesPayment(parse("2016-04-28"), 10.50),
            DesPayment(parse("2016-03-28"), 10.50))))

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockDesConnector.fetchEmployments(eqTo(matchedCitizen.nino), eqTo(interval))(any(), any()))
        .willReturn(successful(desEmployments))

      val result = await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval)(hc))

      result shouldBe List(
        Payment(10.5, parse("2016-04-28")),
        Payment(10.5, parse("2016-03-28")),
        Payment(10.5, parse("2016-02-28")))
    }

    "Return empty list when there are no payments for a given period" in new Setup {
      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockDesConnector.fetchEmployments(eqTo(matchedCitizen.nino), eqTo(interval))(any(), any()))
        .willReturn(successful(Seq.empty))

      val result = await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval)(hc))

      result shouldBe List.empty
    }

    "propagate MatchNotFoundException when the matchId does not exist" in new Setup {

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willThrow(new MatchNotFoundException)

      intercept[MatchNotFoundException] {
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval)(hc))
      }
    }

    "fail when DES returns an error" in new Setup {

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockDesConnector.fetchEmployments(eqTo(matchedCitizen.nino), eqTo(interval))(any(), any()))
        .willReturn(failed(new RuntimeException("test error")))

      intercept[RuntimeException](await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval)(hc)))
    }

    "retry once if DES returns a 503" in new Setup {
      val desEmployments = Seq(DesEmployment(Seq(DesPayment(parse("2016-02-28"), 10.50))))

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockDesConnector.fetchEmployments(eqTo(matchedCitizen.nino), eqTo(interval))(any(), any()))
        .willReturn(Future.failed(Upstream5xxResponse("""¯\_(ツ)_/¯""", 503, 503)))
        .willReturn(successful(desEmployments))

      val result = await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval)(hc))
      result shouldBe List(Payment(10.5, parse("2016-02-28")))

      verify(mockDesConnector, times(2)).fetchEmployments(eqTo(matchedCitizen.nino), eqTo(interval))(any(), any())
    }

    "return a cached value if it exists" in {
      val employments = Seq(DesEmployment(Seq(DesPayment(new LocalDate(2016, 1, 1), 1))))

      val mockMatching = mock[IndividualsMatchingApiConnector]
      given(mockMatching.resolve(eqTo(matchedCitizen.matchId))(any())).willReturn(successful(matchedCitizen))

      val mockDes = mock[DesConnector]
      val stubCache = new PayeIncomeCache(null, null) {
        override def get[T: Format](cacheId: CacheId, fallbackFunction: => Future[T]): Future[T] =
          Future.successful(employments.asInstanceOf[T])
      }

      val testService = new LiveIncomeService(mockMatching, mockDes, 1, stubCache)

      val res = await(
        testService.fetchIncomeByMatchId(matchedCitizen.matchId, toInterval("2016-01-01", "2018-01-01"))(
          HeaderCarrier()))
      res shouldBe employments.flatMap(DesEmployments.toPayments)

      verify(mockDes, never).fetchEmployments(any(), any())(any(), any())
    }
  }

  "SandboxIncomeService fetch income by matchId function" should {

    "return income for the entire available history ordered by date descending" in new Setup {

      val expected = List(
        Payment(500.25, parse("2020-02-16"), Some(EmpRef.fromIdentifiers("123/DI45678")), None, Some(46)),
        Payment(500.25, parse("2020-02-09"), Some(EmpRef.fromIdentifiers("123/DI45678")), None, Some(45)),
        Payment(1000.25, parse("2019-05-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(2), None),
        Payment(1000.25, parse("2019-04-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(1), None),
        Payment(1000.5, parse("2019-03-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(12), None),
        Payment(1000.5, parse("2019-02-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(11), None),
        Payment(1000.5, parse("2019-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10), None)
      )

      val result =
        await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2019-01-01", "2020-03-01"))(hc))
      result shouldBe expected
    }

    "return income for a limited period" in new Setup {

      val expected = List(
        Payment(1000.25, parse("2019-05-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(2), None),
        Payment(1000.25, parse("2019-04-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(1), None),
        Payment(1000.5, parse("2019-03-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(12), None),
        Payment(1000.5, parse("2019-02-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(11), None),
        Payment(1000.5, parse("2019-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10), None)
      )

      val result =
        await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2019-01-01", "2019-07-01"))(hc))
      result shouldBe expected
    }

    "return correct income when range includes a period of no payments" in new Setup {

      val expected = List(
        Payment(500.25, parse("2020-02-09"), Some(EmpRef.fromIdentifiers("123/DI45678")), weekPayNumber = Some(45)),
        Payment(1000.25, parse("2019-05-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), monthPayNumber = Some(2))
      )

      val result =
        await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2019-04-30", "2020-02-15"))(hc))

      result shouldBe expected
    }

    "return no income when the individual has no income for a given period" in new Setup {

      val result =
        await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2016-08-01", "2016-09-01"))(hc))

      result shouldBe Seq.empty
    }

    "throw not found exception when no individual exists for the given matchId" in new Setup {
      intercept[MatchNotFoundException](
        await(sandboxIncomeService.fetchIncomeByMatchId(UUID.randomUUID(), toInterval("2016-01-01", "2018-03-01"))(hc)))
    }
  }
}
