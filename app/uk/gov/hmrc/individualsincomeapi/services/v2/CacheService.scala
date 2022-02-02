/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.Interval
import play.api.libs.json.Format
import uk.gov.hmrc.individualsincomeapi.cache.v2.{CacheRepositoryConfiguration, ShortLivedCache}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CacheService @Inject() (shortLivedCache: ShortLivedCache, conf: CacheRepositoryConfiguration) {

  lazy val cacheEnabled: Boolean = conf.cacheEnabled

  def get[T: Format](cacheId: CacheIdBase, fallbackFunction: => Future[T]): Future[T] =
    if (cacheEnabled) shortLivedCache.fetchAndGetEntry[T](cacheId.id) flatMap {
      case Some(value) =>
        Future.successful(value)
      case None =>
        fallbackFunction map { result =>
          shortLivedCache.cache(cacheId.id, result)
          result
        }
    } else {
      fallbackFunction
    }

}

// Cache ID implementations
// This can then be concatenated for multiple scopes.
// Example;
// read:scope-1 =  [A, B, C]
// read:scope-2 = [D, E, F]
// The cache key (if two scopes alone) would be;
// `id + from + to +  [A, B, C, D, E, F]` Or formatted to `id-from-to-ABCDEF`
// The `fields` param is obtained with scopeService.getValidFieldsForCacheKey(scopes: List[String])

trait CacheIdBase {
  val id: String

  override def toString: String = id
}

case class PayeCacheId(matchId: UUID, interval: Interval, fields: String) extends CacheIdBase {

  lazy val id: String =
    s"$matchId-${interval.getStart}-${interval.getEnd}-$fields"

}

case class SaCacheId(matchId: UUID, interval: TaxYearInterval, fields: String) extends CacheIdBase {

  lazy val id: String =
    s"$matchId-${interval.fromTaxYear.endYr}-${interval.toTaxYear.endYr}-$fields"

}
