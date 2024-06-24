/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfPayeEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2.Income
import uk.gov.hmrc.individualsincomeapi.util.Interval

import java.util.UUID
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeService @Inject() (
  matchingConnector: IndividualsMatchingApiConnector,
  ifConnector: IfConnector,
  @Named("retryDelay") retryDelay: Int,
  cache: CacheService,
  scopeService: ScopesService,
  scopesHelper: ScopesHelper
)(implicit ec: ExecutionContext) {

  private def endpoints = List("paye")

  def fetchIncomeByMatchId(matchId: UUID, interval: Interval, scopes: Iterable[String])(implicit
    hc: HeaderCarrier,
    request: RequestHeader
  ): Future[Seq[Income]] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      payeIncome <- cache.get(
                      PayeCacheId(matchId, interval, scopeService.getValidFieldsForCacheKey(scopes.toList, endpoints)),
                      withRetry(
                        ifConnector.fetchPayeIncome(
                          ninoMatch.nino,
                          interval,
                          Option(scopesHelper.getQueryStringFor(scopes.toList, endpoints)).filter(_.nonEmpty),
                          matchId.toString
                        )
                      )
                    )
    } yield (payeIncome map IfPayeEntry.toIncome).sortBy(_.paymentDate).reverse

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case UpstreamErrorResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }
}
