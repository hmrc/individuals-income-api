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

package unit.uk.gov.hmrc.individualsincomeapi.services.v2

import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.BDDMockito.given
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Format
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.cache.v2.CacheConfiguration
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry
import uk.gov.hmrc.individualsincomeapi.domain.v1.MatchedCitizen
import uk.gov.hmrc.individualsincomeapi.domain.v2.Income
import uk.gov.hmrc.individualsincomeapi.services.v2.{CacheIdBase, LiveIncomeService, PayeIncomeCacheService, SandboxIncomeService, ScopesHelper, ScopesService}
import unit.uk.gov.hmrc.individualsincomeapi.util.TestDates
import utils.{IncomePayeHelpers, SpecBase}
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomeData._
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomePaye

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class IncomeServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with TestDates with IncomePayeHelpers {

  trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val config = mock[CacheConfiguration]

    val stubCache = new PayeIncomeCacheService(null, config) {
      override def get[T: Format](cacheId: CacheIdBase, fallbackFunction: => Future[T])(implicit hc: HeaderCarrier) =
        fallbackFunction
    }

    val mockMatchingConnector = mock[IndividualsMatchingApiConnector]
    val mockIfConnector = mock[IfConnector]
    val scopesService = mock[ScopesService]
    val scopesHelper = mock[ScopesHelper]

    val liveIncomeService = new LiveIncomeService(
      mockMatchingConnector,
      mockIfConnector,
      1,
      stubCache,
      scopesService,
      scopesHelper
    )

    val sandboxIncomeService = new SandboxIncomeService()
  }

  "liveIncomeService fetch income by matchId function" should {

    val matchedCitizen = MatchedCitizen(UUID.randomUUID(), Nino("AA100009B"))
    val interval = toInterval("2016-01-12", "2016-03-22")

    "return the employment's income" in new Setup {

      val ifPaye = Seq(createValidPayeEntry())
      val scopes = Iterable("scope1")

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockIfConnector.fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any()))
        .willReturn(successful(ifPaye))

      val result =
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval, scopes)(hc, FakeRequest()))

      result shouldBe (ifPaye map IfPayeEntry.toIncome)
    }

    "Sort the payments by payment date descending" in new Setup {

      val ifPaye = Seq(
        IfPayeEntry(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some("2006-02-27"),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None
        ),
        IfPayeEntry(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some("2006-05-27"),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None
        )
      )

      val scopes = Iterable("scope1")

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockIfConnector.fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any()))
        .willReturn(successful(ifPaye))

      val result =
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval, scopes)(hc, FakeRequest()))

      result shouldBe List(
        Income(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some("2006-05-27"),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None),
        Income(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          Some("2006-02-27"),
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None)
      )
    }

    "Return empty list when there are no payments for a given period" in new Setup {

      val scopes = Iterable("scope1")

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockIfConnector.fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any()))
        .willReturn(successful(Seq.empty))

      val result =
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval, scopes)(hc, FakeRequest()))

      result shouldBe List.empty
    }

    "propagate MatchNotFoundException when the matchId does not exist" in new Setup {

      val scopes = Iterable("scope1")

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willThrow(new MatchNotFoundException)

      intercept[MatchNotFoundException] {
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval, scopes)(hc, FakeRequest()))
      }
    }

    "fail when IF returns an error" in new Setup {

      val scopes = Iterable("scope1")

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockIfConnector.fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any()))
        .willReturn(failed(new RuntimeException("test error")))

      intercept[RuntimeException](
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval, scopes)(hc, FakeRequest())))
    }

    "retry once if IF returns a 503" in new Setup {

      val ifPaye = Seq(createValidPayeEntry())
      val scopes = Iterable("scope1")

      given(mockMatchingConnector.resolve(matchedCitizen.matchId)).willReturn(successful(matchedCitizen))
      given(mockIfConnector.fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any()))
        .willReturn(Future.failed(Upstream5xxResponse("""¯\_(ツ)_/¯""", 503, 503)))
        .willReturn(successful(ifPaye))

      val result =
        await(liveIncomeService.fetchIncomeByMatchId(matchedCitizen.matchId, interval, scopes)(hc, FakeRequest()))
      result shouldBe (ifPaye map IfPayeEntry.toIncome)

      verify(mockIfConnector, times(2))
        .fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any())
    }

    "return a cached value if it exists" in {

      val paye = Seq(createValidPayeEntry())
      val mockMatching = mock[IndividualsMatchingApiConnector]
      val scopes = Iterable("scope1")
      val mockIf = mock[IfConnector]
      val scopesHelper = mock[ScopesHelper]
      val scopesService = mock[ScopesService]
      val config = mock[CacheConfiguration]

      val stubCache = new PayeIncomeCacheService(null, config) {
        override def get[T: Format](cacheId: CacheIdBase, fallbackFunction: => Future[T])(
          implicit hc: HeaderCarrier): Future[T] =
          Future.successful(paye.asInstanceOf[T])
      }

      given(mockMatching.resolve(eqTo(matchedCitizen.matchId))(any())).willReturn(successful(matchedCitizen))

      val testService = new LiveIncomeService(mockMatching, mockIf, 1, stubCache, scopesService, scopesHelper)

      val res = await(
        testService
          .fetchIncomeByMatchId(matchedCitizen.matchId, toInterval("2016-01-01", "2018-01-01"), scopes)(
            HeaderCarrier(),
            FakeRequest()))
      res shouldBe (paye map IfPayeEntry.toIncome)

      verify(mockIf, never).fetchPayeIncome(eqTo(matchedCitizen.nino), eqTo(interval), any())(any(), any(), any())
    }
  }

  "SandboxIncomeService fetch income by matchId function" should {

    "return income for the entire available history ordered by date descending" in new Setup {

      val ifPaye = Seq(
        SandboxIncomePaye().createValidPayeEntry("2019-05-27"),
        SandboxIncomePaye().createValidPayeEntry("2019-02-27")
      )

      val scopes = Iterable("scope1")

      val result = await(
        sandboxIncomeService.fetchIncomeByMatchId(
          sandboxMatchId,
          toInterval("2019-01-01", "2020-03-01"),
          scopes
        )(hc, FakeRequest())
      )
      result shouldBe (ifPaye map IfPayeEntry.toIncome)
    }

    "return income for a limited period" in new Setup {

      val expected = Seq(
        SandboxIncomePaye().createValidPayeEntry("2019-05-27")
      )

      val scopes = Iterable("scope1")

      val result =
        await(
          sandboxIncomeService
            .fetchIncomeByMatchId(sandboxMatchId, toInterval("2019-05-01", "2019-05-30"), scopes)(hc, FakeRequest()))
      result shouldBe (expected map IfPayeEntry.toIncome)
    }

    "return no income when the individual has no income for a given period" in new Setup {

      val scopes = Iterable("scope1")

      val result =
        await(
          sandboxIncomeService
            .fetchIncomeByMatchId(sandboxMatchId, toInterval("2016-08-01", "2016-09-01"), scopes)(hc, FakeRequest()))

      result shouldBe Seq.empty
    }

    "throw not found exception when no individual exists for the given matchId" in new Setup {

      val scopes = Iterable("scope1")

      intercept[MatchNotFoundException](
        await(sandboxIncomeService
          .fetchIncomeByMatchId(UUID.randomUUID(), toInterval("2016-01-01", "2018-03-01"), scopes)(hc, FakeRequest())))
    }
  }
}
