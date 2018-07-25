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

package uk.gov.hmrc.individualsincomeapi.cache

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.http.cache.client.{ShortLivedCache, ShortLivedHttpCaching}
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePut}
import uk.gov.hmrc.individualsincomeapi.config.WSHttp
import uk.gov.hmrc.play.config.inject.ServicesConfig

@Singleton
class CachingClient @Inject()(val shortLiveCache: HttpCaching) extends ShortLivedCache {
  override implicit val crypto: CompositeSymmetricCrypto = ApplicationCrypto.JsonCrypto
}

@Singleton
class HttpCaching @Inject()(conf: ServicesConfig) extends ShortLivedHttpCaching {
  override def defaultSource: String = "ogd-apis" // share cache instance with individuals-employments-api
  override def baseUri: String = conf.baseUrl("cacheable.short-lived-cache")
  override def domain: String = conf.getConfString(
    "cacheable.short-lived-cache.domain",
    throw new RuntimeException("missing configuration cacheable.short-lived-cache.domain")
  )
  override def http: CoreGet with CorePut with CoreDelete = WSHttp
}