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

package uk.gov.hmrc.individualsincomeapi.domain

import java.util.UUID

import org.joda.time.LocalDate.parse
import org.joda.time.{Interval, LocalDate}
import uk.gov.hmrc.domain.{EmpRef, Nino, SaUtr}

case class MatchedCitizen(matchId: UUID, nino: Nino)


case class Individual(matchId: UUID,
                      nino: String,
                      firstName: String,
                      lastName: String,
                      dateOfBirth: LocalDate,
                      income: Seq[Payment],
                      saIncome: Seq[DesSAIncome])

case class Payment(taxablePayment: Double,
                   paymentDate: LocalDate,
                   employerPayeReference: Option[EmpRef] = None,
                   monthPayNumber: Option[Int] = None,
                   weekPayNumber: Option[Int] = None) {

  def isPaidWithin(interval: Interval): Boolean =
    interval.contains(paymentDate.toDateTimeAtStartOfDay)

}

object SandboxIncomeData {

  def findByMatchId(matchId: UUID) = individuals.find(_.matchId == matchId)

  def matchedCitizen(matchId: UUID) = matchId match {
    case `sandboxMatchId` => Some(MatchedCitizen(sandboxMatchId, sandboxNino))
    case _ => None
  }

  private lazy val individuals = Seq(amanda())

  val sandboxNino = Nino("NA000799C")

  val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")

  val acmeEmployerReference = EmpRef.fromIdentifiers("123/AI45678")

  val disneyEmployerReference = EmpRef.fromIdentifiers("123/DI45678")

  val sandboxUtr = SaUtr("2432552635")

  private def amanda() = Individual(
    sandboxMatchId,
    sandboxNino.nino,
    "Amanda",
    "Joseph",
    parse("1960-01-15"),
    Seq(
      Payment(1000.50, parse("2016-01-28"), Some(acmeEmployerReference), monthPayNumber = Some(10)),
      Payment(1000.50, parse("2016-02-28"), Some(acmeEmployerReference), monthPayNumber = Some(11)),
      Payment(1000.50, parse("2016-03-28"), Some(acmeEmployerReference), monthPayNumber = Some(12)),
      Payment(1000.25, parse("2016-04-28"), Some(acmeEmployerReference), monthPayNumber = Some(1)),
      Payment(1000.25, parse("2016-05-28"), Some(acmeEmployerReference), monthPayNumber = Some(2)),
      Payment(500.25, parse("2017-02-09"), Some(disneyEmployerReference), weekPayNumber = Some(45)),
      Payment(500.25, parse("2017-02-16"), Some(disneyEmployerReference), weekPayNumber = Some(46))),
    Seq(
      DesSAIncome("2014", Seq(DesSAReturn(parse("2012-01-06"), parse("2014-06-06"), sandboxUtr, Some(5000), Some(10500), Some(30000), Some(2143.32), Some(1054.65)))),
      DesSAIncome("2015", Seq(DesSAReturn(parse("2012-01-06"), parse("2015-10-06"), sandboxUtr)))
    )
  )
}
