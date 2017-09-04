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

package unit.uk.gov.hmrc.individualsincomeapi.util

import org.joda.time.DateTime
import org.joda.time.LocalDate.parse
import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.individualsincomeapi.domain.ValidationException
import uk.gov.hmrc.individualsincomeapi.util.Dates

class DatesSpec extends FlatSpec with Matchers {

  "Dates utility" should "derive an interval between two dates" in {
    val (fromDate, toDate) = (parse("2020-01-01"), parse("2020-01-02"))
    Dates.toInterval(fromDate, toDate).toString shouldBe "2020-01-01T00:00:00.000Z/2020-01-02T00:00:00.001Z"
  }

  it should "fail to derive an interval with a from date which is before 31st March 2013" in {
    val (fromDate, toDate) = (parse("2013-03-30"), parse("2020-01-02"))
    the[ValidationException] thrownBy {
      Dates.toInterval(fromDate, toDate)
    } should have message "fromDate earlier than 31st March 2013"

    noException should be thrownBy Dates.toInterval(parse("2013-03-31"), toDate)
  }

  it should "format date time instances" in {
    Dates.toFormattedLocalDate(DateTime.parse("2017-12-31")) shouldBe "2017-12-31"
  }

}
