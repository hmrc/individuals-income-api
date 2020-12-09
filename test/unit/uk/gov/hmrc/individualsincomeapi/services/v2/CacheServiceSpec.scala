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

package unit.uk.gov.hmrc.individualsincomeapi.services.v2

import java.util.UUID

import it.uk.gov.hmrc.individualsincomeapi.cache.v2.services.TestCacheId
import org.joda.time.{Interval, LocalDate}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{verify, verifyNoInteractions}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.cache.v2.{CacheConfiguration, ShortLivedCache}
import uk.gov.hmrc.individualsincomeapi.domain.{TaxYear, TaxYearInterval}
import uk.gov.hmrc.individualsincomeapi.services.v2.{CacheIdBase, CacheService, PayeCacheId, SaCacheId}
import utils.TestSupport

import scala.concurrent.Future

class CacheServiceSpec extends TestSupport with MockitoSugar with ScalaFutures {

  val cacheId = TestCacheId("foo")
  val cachedValue = TestClass("cached value")
  val newValue = TestClass("new value")

  trait Setup {

    val mockClient = mock[ShortLivedCache]
    val mockCacheConfig = mock[CacheConfiguration]
    val cacheService = new CacheService {
      override val shortLivedCache: ShortLivedCache = mockClient
      override val conf: CacheConfiguration = mockCacheConfig
      override val key: String = "test"
    }

    implicit val hc: HeaderCarrier = HeaderCarrier()

    given(mockCacheConfig.cacheEnabled).willReturn(true)

  }

  "cacheService.get" should {

    "return the cached value for a given id and key" in new Setup {

      given(mockClient.fetchAndGetEntry[TestClass](eqTo(cacheId.id), eqTo(cacheService.key))(any()))
        .willReturn(Future.successful(Some(cachedValue)))
      await(cacheService.get[TestClass](cacheId, Future.successful(newValue))) shouldBe cachedValue

    }

    "cache the result of the fallback function when no cached value exists for a given id and key" in new Setup {

      given(mockClient.fetchAndGetEntry[TestClass](eqTo(cacheId.id), eqTo(cacheService.key))(any()))
        .willReturn(Future.successful(None))

      await(cacheService.get[TestClass](cacheId, Future.successful(newValue))) shouldBe newValue
      verify(mockClient).cache[TestClass](eqTo(cacheId.id), eqTo(cacheService.key), eqTo(newValue))(any())

    }

    "ignore the cache when caching is not enabled" in new Setup {

      given(mockCacheConfig.cacheEnabled).willReturn(false)
      await(cacheService.get[TestClass](cacheId, Future.successful(newValue))) shouldBe newValue
      verifyNoInteractions(mockClient)

    }
  }

  "PayeCacheId" should {

    "produce a cache id based on matchId and scopes" in {

      val matchId = UUID.randomUUID()
      val fromDateString = "2017-03-02"
      val toDateString = "2017-05-31"

      val interval = new Interval(
        new LocalDate(fromDateString).toDateTimeAtStartOfDay,
        new LocalDate(toDateString).toDateTimeAtStartOfDay)

      val fields = "ABDFH"

      PayeCacheId(matchId, interval, fields).id shouldBe
        s"$matchId-${interval.getStart}-${interval.getEnd}-ABDFH"

    }

  }

  "SaCacheId" should {

    "produce a cache id based on nino and scopes" in {

      val nino = Nino("NA000799C")
      val interval = TaxYearInterval(TaxYear("2015-16"), TaxYear("2016-17"))

      val fields = "ABCDGK"

      SaCacheId(nino, interval, fields).id shouldBe
        "NA000799C-2016-2017-ABCDGK"

    }

  }
}

case class TestCacheId(id: String) extends CacheIdBase

case class TestClass(value: String)

object TestClass {

  implicit val format: OFormat[TestClass] = Json.format[TestClass]

}
