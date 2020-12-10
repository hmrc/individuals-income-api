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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework

import java.util.UUID

import org.joda.time.LocalDate.parse
import org.joda.time.{Interval, LocalDate}
import uk.gov.hmrc.domain.{EmpRef, Nino, SaUtr}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye.{IfBenefits, IfEmployeeNics, IfEmployeePensionContribs, IfGrossEarningsForNics, IfPayeEntry, IfPostGradLoan, IfStudentLoan, IfTotalEmployerNics}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa.{IfAddress, IfSaEntry, IfSaIncome, IfSaReturn}

case class MatchedCitizen(matchId: UUID, nino: Nino)

case class Individual(
  matchId: UUID,
  nino: String,
  firstName: String,
  lastName: String,
  dateOfBirth: LocalDate,
  income: Seq[IfPayeEntry],
  saIncome: Seq[IfSaEntry])

object SandboxIncomeData {

  def findByMatchId(matchId: UUID) = individuals.find(_.matchId == matchId)

  def matchedCitizen(matchId: UUID) = matchId match {
    case `sandboxMatchId` => Some(MatchedCitizen(sandboxMatchId, sandboxNino))
    case _                => None
  }

  private lazy val individuals = Seq(amanda())

  val sandboxNino = Nino("NA000799C")

  val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")

  val acmeEmployerReference = EmpRef.fromIdentifiers("123/AI45678")

  val disneyEmployerReference = EmpRef.fromIdentifiers("123/DI45678")

  val sandboxUtr = SaUtr("2432552635")

  private def amanda() =
    Individual(
      sandboxMatchId,
      sandboxNino.nino,
      "Amanda",
      "Joseph",
      parse("1960-01-15"),
      Seq(IncomePayeHelpers().createValidPayeEntry()),
      Seq(IncomeSaHelpers().createValidSaTaxYearEntry())
    )
}

case class IncomePayeHelpers() {
  def createValidPayeEntry() =
    IfPayeEntry(
      Some("K971"),
      Some("36"),
      Some(19157.5),
      Some(3095.89),
      Some(159228.49),
      Some(createValodIFGrossEarningsForNics),
      Some("345/34678"),
      Some("2006-02-27"),
      Some(16533.95),
      Some("18-19"),
      Some("3"),
      Some("2"),
      Some("W4"),
      Some(198035.8),
      Some(createValidTotalEmployerNics()),
      Some(createValidEmployeeNics()),
      Some(createValidEmployeePensionContribs()),
      Some(createValidBenefits()),
      Some(39708.7),
      Some(createValidStudentLoan()),
      Some(createValidPostGradLoan())
    )

  private def createValidEmployeeNics() =
    IfEmployeeNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  private def createValidTotalEmployerNics() =
    IfTotalEmployerNics(
      Some(15797.45),
      Some(13170.69),
      Some(16193.76),
      Some(30846.56),
      Some(10633.5),
      Some(15579.18),
      Some(110849.27),
      Some(162081.23)
    )

  private def createValidEmployeePensionContribs() =
    IfEmployeePensionContribs(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))

  private def createValidBenefits() = IfBenefits(Some(506328.1), Some(246594.83))

  private def createValidStudentLoan() = IfStudentLoan(Some("02"), Some(88478), Some(545))

  private def createValidPostGradLoan() = IfPostGradLoan(Some(15636), Some(46849))

  private def createValodIFGrossEarningsForNics() =
    IfGrossEarningsForNics(Some(169731.51), Some(173987.07), Some(822317.49), Some(818841.65))
}

case class IncomeSaHelpers() {
  def createValidSaTaxYearEntry() = {
    val returnTypeList = Seq(createValidSaReturnType())
    IfSaEntry(Some("2020"), Some(100.01), Some(returnTypeList))
  }

  private def createValidSaReturnType() = {
    val validSaIncome = IfSaIncome(
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0),
      Some(100.0)
    )

    IfSaReturn(
      Some("1234567890"),
      Some("2020-01-01"),
      Some("2020-01-01"),
      Some("This is a business description"),
      Some("12345678901"),
      Some("2020-01-01"),
      Some("2020-01-30"),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(100.01),
      Some(IfAddress(Some("line1"), Some("line2"), Some("line3"), Some("line4"), None, Some("QW123QW"))),
      Some(validSaIncome)
    )
  }
}
