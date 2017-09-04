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

package uk.gov.hmrc.individualsincomeapi.services

import java.util.UUID
import javax.inject.Singleton

import org.joda.time.{Interval, LocalDate}
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.findByMatchId
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, Payment}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait IncomeService {
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isBefore _)

  def fetchIncomeByMatchId(matchId: UUID, interval: Interval)(implicit hc: HeaderCarrier): Future[Seq[Payment]]
}

@Singleton
class SandboxIncomeService extends IncomeService {

  def paymentFilter(interval: Interval)(payment: Payment): Boolean = {
    val paymentDate = payment.paymentDate.toDateTimeAtStartOfDay
    interval.contains(paymentDate) || interval.getEnd().isEqual(paymentDate)
  }

  override def fetchIncomeByMatchId(matchId: UUID, interval: Interval)(implicit hc: HeaderCarrier): Future[Seq[Payment]] = {
    findByMatchId(matchId).map(_.income) match {
      case Some(payments) =>
        successful(payments.filter(paymentFilter(interval)).sortBy(_.paymentDate).reverse)
      case None => failed(new MatchNotFoundException)
    }
  }
}