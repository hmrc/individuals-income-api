/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json.Format
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class CacheService(shortLivedCache: ShortLivedCache, configuration: CacheConfiguration) {

  val key: String

  def get[T](cacheId: CacheId, fallbackFunction: => Future[T])(implicit formats: Format[T]): Future[T] = {

    if (configuration.enabled) shortLivedCache.fetch[T](cacheId.id, key) flatMap {
      case Some(value) =>
        Future.successful(value)
      case None =>
        fallbackFunction map { result =>
          shortLivedCache.cache[T](cacheId.id, key, result)
          result
        }
    } else {
      fallbackFunction
    }
  }
}

@Singleton
class SaIncomeCacheService @Inject()(shortLivedCache: ShortLivedCache, configuration: CacheConfiguration)
  extends CacheService(shortLivedCache, configuration) {

  override val key = "sa-income"
}

trait CacheId {
  val id: String

  override def toString = id
}

case class SaCacheId(nino: Nino, interval: TaxYearInterval) extends CacheId {
  lazy val id = s"${nino.nino}-${interval.fromTaxYear.endYr}-${interval.toTaxYear.endYr}"
}