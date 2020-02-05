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

package it.uk.gov.hmrc.individualsincomeapi.connectors

import java.util.UUID

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json, OFormat}
import uk.gov.hmrc.individualsincomeapi.cache.ShortLivedCache
import uk.gov.hmrc.mongo.MongoSpecSupport
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.integration.ServiceSpec
import utils.TestSupport

class ShortLivedCacheSpec
    extends WordSpec with Matchers with MongoSpecSupport with ServiceSpec with BeforeAndAfterEach with TestSupport {

  val cacheTtl = 60
  val id = UUID.randomUUID().toString
  val cachekey = "test-class-key"
  val testValue = TestClass("one", "two")

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> mongoUri, "cache.ttlInSeconds" -> cacheTtl)
    .bindings(bindModules: _*)
    .build()

  val shortLivedCache = fakeApplication.injector.instanceOf[ShortLivedCache]

  def externalServices: Seq[String] = Seq.empty

  override def beforeEach() {
    super.beforeEach()
    await(shortLivedCache.drop)
  }

  override def afterEach() {
    super.afterEach()
    await(shortLivedCache.drop)
  }

  "cache" should {
    "store the encrypted version of a value" in {
      await(shortLivedCache.cache(id, cachekey, testValue)(TestClass.format))
      retrieveRawCachedValue(id, cachekey) shouldBe JsString("6aZpkTxkw3C4e5xTyfy3Lf/OZOFz+GcaSkeFI++0HOs=")
    }

    "update a cached value for a given id and key" in {
      val newValue = TestClass("three", "four")

      await(shortLivedCache.cache(id, cachekey, testValue)(TestClass.format))
      retrieveRawCachedValue(id, cachekey) shouldBe JsString("6aZpkTxkw3C4e5xTyfy3Lf/OZOFz+GcaSkeFI++0HOs=")

      await(shortLivedCache.cache(id, cachekey, newValue)(TestClass.format))
      retrieveRawCachedValue(id, cachekey) shouldBe JsString("8jVeGr+Ivyk5mkBj2VsQE3G+oPGXoYejrSp5hfVAPYU=")
    }
  }

  "fetch" should {
    "retrieve the unencrypted cached value for a given id and key" in {
      await(shortLivedCache.cache(id, cachekey, testValue)(TestClass.format))
      await(shortLivedCache.fetchAndGetEntry[TestClass](id, cachekey)(TestClass.format)) shouldBe Some(testValue)
    }

    "return None if no cached value exists for a given id and key" in {
      await(shortLivedCache.fetchAndGetEntry[TestClass](id, cachekey)(TestClass.format)) shouldBe None
    }
  }

  private def retrieveRawCachedValue(id: String, key: String) = {
    val storedValue = await(shortLivedCache.findById(id)).get
    (storedValue.data.get \ cachekey).get
  }

  case class TestClass(one: String, two: String)

  object TestClass {
    implicit val format: OFormat[TestClass] = Json.format[TestClass]
  }
}
