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

package it.uk.gov.hmrc.individualsincomeapi.cache.v2.services

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.services.v2.{CacheIdBase, CacheService}

import scala.concurrent.Future

class CacheServiceSpec
    extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues with IntegrationPatience {

  implicit val hc = HeaderCarrier()

  "cache service" - {

    "sa" - {

      "must fetch an entry" in {

        val app = new GuiceApplicationBuilder().build()

        val cacheId = TestCacheId("foo")

        running(app) {

          val svc = app.injector.instanceOf[CacheService]

          svc
            .get(cacheId, Future.successful(TestClass("bar")))
            .futureValue mustEqual TestClass("bar")

        }
      }

      "must fetch an existing entry when not expired " in {

        val app = new GuiceApplicationBuilder().build()

        val cacheId1 = TestCacheId("foo")
        val cacheId2 = TestCacheId("bar")

        running(app) {

          val svc = app.injector.instanceOf[CacheService]

          svc
            .get(cacheId1, Future.successful(TestClass("bar")))
            .futureValue mustEqual TestClass("bar")
          svc
            .get(cacheId1, Future.successful(TestClass("miss")))
            .futureValue mustEqual TestClass("bar")
          svc
            .get(cacheId2, Future.successful(TestClass("miss")))
            .futureValue mustEqual TestClass("miss")

        }
      }
    }

    "paye" - {

      "must fetch an entry" in {

        val app = new GuiceApplicationBuilder().build()

        val cacheId = TestCacheId("one")

        running(app) {

          val svc = app.injector.instanceOf[CacheService]

          svc
            .get(cacheId, Future.successful(TestClass("bar")))
            .futureValue mustEqual TestClass("bar")

        }
      }

      "must fetch an existing entry when not expired " in {

        val app = new GuiceApplicationBuilder().build()

        val cacheId1 = TestCacheId("one")
        val cacheId2 = TestCacheId("two")

        running(app) {

          val svc = app.injector.instanceOf[CacheService]

          svc
            .get(cacheId1, Future.successful(TestClass("foo")))
            .futureValue mustEqual TestClass("bar")
          svc
            .get(cacheId1, Future.successful(TestClass("miss")))
            .futureValue mustEqual TestClass("bar")
          svc
            .get(cacheId2, Future.successful(TestClass("miss")))
            .futureValue mustEqual TestClass("miss")

        }
      }
    }
  }
}

case class TestCacheId(id: String) extends CacheIdBase

case class TestClass(param: String)

object TestClass {

  implicit val format: OFormat[TestClass] = Json.format[TestClass]

}
