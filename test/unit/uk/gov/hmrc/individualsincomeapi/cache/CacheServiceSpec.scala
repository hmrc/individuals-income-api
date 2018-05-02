/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsincomeapi.cache

import java.util.UUID

import org.mockito.BDDMockito.given
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.individualsincomeapi.cache.{CacheConfiguration, CacheId, CacheService, ShortLivedCache}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.successful

class CacheServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val cacheId = TestCacheId(UUID.randomUUID().toString)
  val cachedValue = TestClass("cached value")
  val newValue = TestClass("new value")

  trait Setup {
    val mockShortLivedCache = mock[ShortLivedCache]
    val configuration = mock[CacheConfiguration]
    val cacheService = new TestCacheService(mockShortLivedCache, configuration)
    given(configuration.enabled).willReturn(true)
  }

  "cacheService.get" should {
    "return the cached value for a given id and key" in new Setup {
      given(mockShortLivedCache.fetch[TestClass](cacheId.id, cacheService.key)(TestClass.format)).willReturn(successful(Some(cachedValue)))
      await(cacheService.get[TestClass](cacheId, successful(newValue))) shouldBe cachedValue
    }

    "cache the result of the fallback function when no cached value exists for a given id and key" in new Setup {
      given(mockShortLivedCache.fetch[TestClass](cacheId.id, cacheService.key)(TestClass.format)).willReturn(successful(None))
      await(cacheService.get[TestClass](cacheId, successful(newValue))) shouldBe newValue
      verify(mockShortLivedCache).cache[TestClass](cacheId.id, cacheService.key, newValue)
    }

    "ignore the cache when caching is not enabled" in new Setup {
      given(configuration.enabled).willReturn(false)
      await(cacheService.get[TestClass](cacheId, successful(newValue))) shouldBe newValue
      verifyZeroInteractions(mockShortLivedCache)
    }
  }
}

class TestCacheService(shortLivedCache: ShortLivedCache, configuration: CacheConfiguration)
  extends CacheService(shortLivedCache, configuration) {

  override val key = "test-key"
}

case class TestCacheId(id: String) extends CacheId

case class TestClass(value: String)

object TestClass {
  implicit val format = Json.format[TestClass]
}