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

package uk.gov.hmrc.individualsincomeapi.services.v2

import java.util.UUID

import javax.inject.{Inject, Singleton}
import org.joda.time.Interval
import play.api.libs.json.Format
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.cache.v2.{CacheConfigurationV2, ShortLivedCacheV2}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval
import uk.gov.hmrc.individualsincomeapi.util.CacheKeyHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CacheServiceV2 {

  val shortLivedCache: ShortLivedCacheV2
  val conf: CacheConfigurationV2
  val key: String

  lazy val cacheEnabled: Boolean = conf.cacheEnabled

  def get[T: Format](cacheId: String, fallbackFunction: => Future[T])(implicit hc: HeaderCarrier): Future[T] =
    if (cacheEnabled) shortLivedCache.fetchAndGetEntry[T](cacheId, key) flatMap {
      case Some(value) =>
        Future.successful(value)
      case None =>
        fallbackFunction map { result =>
          shortLivedCache.cache(cacheId, key, result)
          result
        }
    } else {
      fallbackFunction
    }

}

@Singleton
class SaIncomeCacheService @Inject()(val shortLivedCache: ShortLivedCacheV2, val conf: CacheConfigurationV2)
    extends CacheServiceV2 {

  val key = conf.saKey

}

@Singleton
class PayeIncomeCache @Inject()(val shortLivedCache: ShortLivedCacheV2, val conf: CacheConfigurationV2)
    extends CacheServiceV2 {

  val key: String = conf.payeKey

}

// Cache ID implementations
// This can then be concatenated for multiple scopes.
// Example;
// read:scope-1 =  [A, B, C]
// read:scope-2 = [D, E, F]
// The cache key (if two scopes alone) would be;
// `id + from + to +  [A, B, C, D, E, F]` Or formatted to `id-from-to-ABCDEF`
// We can base encode the fields to keep the key short

case class CacheId(matchId: UUID, interval: Interval, fields: String) extends CacheKeyHelper {

  lazy val id: String =
    s"$matchId-${interval.getStart}-${interval.getEnd}-${encodeFields(fields)}"

}

case class SaCacheId(nino: Nino, interval: TaxYearInterval, fields: String) extends CacheKeyHelper {

  lazy val id =
    s"${nino.nino}-${interval.fromTaxYear.endYr}-${interval.toTaxYear.endYr}-${encodeFields(fields)}"

}
