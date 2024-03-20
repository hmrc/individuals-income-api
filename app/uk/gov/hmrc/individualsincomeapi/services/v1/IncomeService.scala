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

package uk.gov.hmrc.individualsincomeapi.services.v1

import java.time.LocalDate
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.MatchNotFoundException
import uk.gov.hmrc.individualsincomeapi.domain.des.DesEmployments
import uk.gov.hmrc.individualsincomeapi.domain.v1.JsonFormatters._
import uk.gov.hmrc.individualsincomeapi.domain.v1.Payment
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.findByMatchId
import uk.gov.hmrc.individualsincomeapi.util.Interval

import java.util.UUID
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.Future.{failed, successful}
import scala.concurrent.{ExecutionContext, Future}

trait IncomeService {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def fetchIncomeByMatchId(matchId: UUID, interval: Interval)(implicit hc: HeaderCarrier): Future[Seq[Payment]]
}

@Singleton
class LiveIncomeService @Inject()(
  matchingConnector: IndividualsMatchingApiConnector,
  desConnector: DesConnector,
  @Named("retryDelay") retryDelay: Int,
  cache: CacheService)(implicit ec: ExecutionContext)
    extends IncomeService {

  override def fetchIncomeByMatchId(matchId: UUID, interval: Interval)(
    implicit hc: HeaderCarrier): Future[Seq[Payment]] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      desEmployments <- cache.get(
                         cacheId(matchId, interval),
                         withRetry(desConnector.fetchEmployments(ninoMatch.nino, interval))
                       )
    } yield (desEmployments flatMap DesEmployments.toPayments).sortBy(_.paymentDate).reverse

  private def cacheId(matchId: UUID, interval: Interval) = new CacheId {
    override val id: String = s"$matchId-${interval.getStart}-${interval.getEnd}"
  }

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case UpstreamErrorResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }
}

@Singleton
class SandboxIncomeService extends IncomeService {

  private def paymentFilter(interval: Interval)(payment: Payment): Boolean = {
    val paymentDate = payment.paymentDate.atStartOfDay()
    interval.contains(paymentDate) || interval.getEnd.isEqual(paymentDate)
  }

  override def fetchIncomeByMatchId(matchId: UUID, interval: Interval)(
    implicit hc: HeaderCarrier): Future[Seq[Payment]] =
    findByMatchId(matchId).map(_.income) match {
      case Some(payments) =>
        successful(payments.filter(paymentFilter(interval)).sortBy(_.paymentDate).reverse)
      case None => failed(new MatchNotFoundException)
    }
}
