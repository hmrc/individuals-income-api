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

package unit.uk.gov.hmrc.individualsincomeapi.services

import java.util.UUID

import org.joda.time.LocalDate.parse
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData._
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, Payment}
import uk.gov.hmrc.individualsincomeapi.services.SandboxIncomeService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.individualsincomeapi.util.Dates

class IncomeServiceSpec extends UnitSpec with Dates {

  implicit val hc = HeaderCarrier()
  val sandboxIncomeService = new SandboxIncomeService()

  "SandboxIncomeService fetch income by matchId function" should {

    "return income for the entire available history ordered by date descending" in {

      val expected = List(
        Payment(500.25, parse("2017-02-16"), Some(EmpRef.fromIdentifiers("123/DI45678")), None, Some(46)),
        Payment(500.25, parse("2017-02-09"), Some(EmpRef.fromIdentifiers("123/DI45678")), None, Some(45)),
        Payment(1000.25, parse("2016-05-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(2), None),
        Payment(1000.25, parse("2016-04-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(1), None),
        Payment(1000.5, parse("2016-03-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(12), None),
        Payment(1000.5, parse("2016-02-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(11), None),
        Payment(1000.5, parse("2016-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10), None))

      val result = await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2016-01-01", "2017-03-01"))(hc))
      result shouldBe expected
    }

    "return income for a limited period" in {

      val expected = List(
        Payment(1000.25, parse("2016-05-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(2), None),
        Payment(1000.25, parse("2016-04-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(1), None),
        Payment(1000.5, parse("2016-03-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(12), None),
        Payment(1000.5, parse("2016-02-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(11), None),
        Payment(1000.5, parse("2016-01-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), Some(10), None))

      val result = await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2016-01-01", "2016-07-01"))(hc))
      result shouldBe expected
    }

    "return correct income when range includes a period of no payments" in {

      val expected = List(
        Payment(500.25, parse("2017-02-09"), Some(EmpRef.fromIdentifiers("123/DI45678")), weekPayNumber = Some(45)),
        Payment(1000.25, parse("2016-05-28"), Some(EmpRef.fromIdentifiers("123/AI45678")), monthPayNumber = Some(2)))

      val result = await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2016-04-30", "2017-02-15"))(hc))

      result shouldBe expected
    }

    "return no income when the individual has no income for a given period" in {

      val result = await(sandboxIncomeService.fetchIncomeByMatchId(sandboxMatchId, toInterval("2016-08-01", "2016-09-01"))(hc))

      result shouldBe Seq.empty
    }

    "throw not found exception when no individual exists for the given matchId" in {
      intercept[MatchNotFoundException](
        await(sandboxIncomeService.fetchIncomeByMatchId(UUID.randomUUID(), toInterval("2016-01-01", "2018-03-01"))(hc)))
    }
  }
}
