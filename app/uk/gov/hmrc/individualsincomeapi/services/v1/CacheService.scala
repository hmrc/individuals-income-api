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

package uk.gov.hmrc.individualsincomeapi.services.v1

import play.api.libs.json.Format
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsincomeapi.cache.v1.{CacheRepositoryConfiguration, ShortLivedCache}
import uk.gov.hmrc.individualsincomeapi.domain.TaxYearInterval

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CacheService @Inject() (shortLivedCache: ShortLivedCache, conf: CacheRepositoryConfiguration){

  lazy val cacheEnabled: Boolean = conf.cacheEnabled

  def get[T: Format](cacheId: CacheId, fallbackFunction: => Future[T]): Future[T] =
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

trait CacheId {
  val id: String

  override def toString: String = id
}

case class SaCacheId(nino: Nino, interval: TaxYearInterval) extends CacheId {
  lazy val id = s"${nino.nino}-${interval.fromTaxYear.endYr}-${interval.toTaxYear.endYr}"
}
