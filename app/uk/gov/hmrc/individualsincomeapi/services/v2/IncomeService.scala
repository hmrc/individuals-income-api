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

import javax.inject.{Inject, Named, Singleton}
import org.joda.time.{Interval, LocalDate}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomeData.findByMatchId
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.{IfPaye, IfPayeEntry}
import uk.gov.hmrc.individualsincomeapi.domain.v2.Income

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait IncomeService {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def fetchIncomeByMatchId(matchId: UUID, interval: Interval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[Income]]

  def endpoints =
    List("incomePaye")

  def cacheId = {}
}

@Singleton
class LiveIncomeService @Inject()(
  matchingConnector: IndividualsMatchingApiConnector,
  ifConnector: IfConnector,
  @Named("retryDelay") retryDelay: Int,
  cache: PayeIncomeCacheService,
  scopeService: ScopesService,
  scopesHelper: ScopesHelper)
    extends IncomeService {

  override def fetchIncomeByMatchId(matchId: UUID, interval: Interval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[Income]] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      payeIncome <- cache.get(
                     PayeCacheId(matchId, interval, scopeService.getValidFieldsForCacheKey(scopes.toList, endpoints)),
                     withRetry(
                       ifConnector.fetchPayeIncome(
                         ninoMatch.nino,
                         interval,
                         Option(scopesHelper.getQueryStringFor(scopes.toList, endpoints)).filter(_.nonEmpty)
                       )
                     )
                   )
    } yield (payeIncome map IfPayeEntry.toIncome).sortBy(_.paymentDate).reverse

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case Upstream5xxResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }

}

@Singleton
class SandboxIncomeService extends IncomeService {

  def paymentFilter(interval: Interval)(payeEntry: IfPayeEntry): Boolean = {

    val paymentDate = LocalDate
      .parse(
        payeEntry.paymentDate.getOrElse(LocalDate.now.toString)
      )
      .toDateTimeAtStartOfDay

    interval.contains(paymentDate) || interval.getEnd.isEqual(paymentDate)
  }

  override def fetchIncomeByMatchId(matchId: UUID, interval: Interval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[Income]] =
    findByMatchId(matchId).map(_.income) match {
      case Some(payeIncome) =>
        successful(
          (payeIncome.filter(paymentFilter(interval)) map IfPayeEntry.toIncome).sortBy(_.paymentDate).reverse
        )
      case None => failed(new MatchNotFoundException)
    }
}
