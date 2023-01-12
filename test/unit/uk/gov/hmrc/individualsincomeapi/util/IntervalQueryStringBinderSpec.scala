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

package unit.uk.gov.hmrc.individualsincomeapi.util

import org.joda.time.LocalDateTime
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.EitherValues
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.individualsincomeapi.util.IntervalQueryStringBinder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IntervalQueryStringBinderSpec
    extends AnyFlatSpec with Matchers with EitherValues with TestDates with GuiceOneAppPerSuite {

  lazy val intervalQueryStringBinder = new IntervalQueryStringBinder

  "Interval query string binder" should "fail to bind a missing or malformed fromDate or a malformed toDate parameter" in new TableDrivenPropertyChecks {
    val fixtures = Table(
      ("parameters", "response"),
      (Map[String, Seq[String]]().empty, "fromDate is required"),
      (Map("fromDate" -> Seq.empty[String]), "fromDate is required"),
      (Map("fromDate" -> Seq("")), "fromDate: invalid date format"),
      (Map("fromDate" -> Seq("20200131")), "fromDate: invalid date format"),
      (Map("fromDate" -> Seq("2020-01-31"), "toDate" -> Seq("")), "toDate: invalid date format"),
      (Map("fromDate" -> Seq("2020-01-31"), "toDate" -> Seq("20201231")), "toDate: invalid date format")
    )

    fixtures foreach {
      case (parameters, response) =>
        val maybeEither = intervalQueryStringBinder.bind("", parameters)
        maybeEither shouldBe Some(Left(response))
    }
  }

  it should "default to today's date when a valid fromDate parameter is present but a toDate parameter is missing" in {
    val parameters = Map("fromDate" -> Seq("2017-01-31"))
    val maybeEither = intervalQueryStringBinder.bind("", parameters)
    maybeEither shouldBe Some(
      Right(toInterval("2017-01-31T00:00:00.000", LocalDateTime.now().withTime(0, 0, 0, 1).toString())))
  }

  it should "succeed in binding an interval from well formed fromDate and toDate parameters" in {
    val parameters = Map("fromDate" -> Seq("2020-01-31"), "toDate" -> Seq("2020-12-31"))
    val maybeEither = intervalQueryStringBinder.bind("", parameters)
    maybeEither shouldBe Some(Right(toInterval("2020-01-31T00:00:00.000", "2020-12-31T00:00:00.001")))
  }

  it should "fail to bind an interval from an invalid date range" in {
    val parameters = Map("fromDate" -> Seq("2020-12-31"), "toDate" -> Seq("2020-01-31"))
    val maybeEither = intervalQueryStringBinder.bind("", parameters)
    maybeEither shouldBe Some(Left("Invalid time period requested"))
  }

  it should "bind an interval from a date range beginning after the des data inception date" in {
    val maybeEither =
      intervalQueryStringBinder.bind("", Map("fromDate" -> Seq("2013-04-01"), "toDate" -> Seq("2014-12-31")))
    maybeEither shouldBe Some(Right(toInterval("2013-04-01T00:00:00.000", "2014-12-31T00:00:00.001")))
  }

  it should "bind an interval from an date range beginning on the des data inception date" in {
    val maybeEither =
      intervalQueryStringBinder.bind("", Map("fromDate" -> Seq("2013-03-31"), "toDate" -> Seq("2014-12-31")))
    maybeEither shouldBe Some(Right(toInterval("2013-03-31T00:00:00.000", "2014-12-31T00:00:00.001")))
  }

  it should "fail to bind an interval from an date range beginning before the des data inception date" in {
    val maybeEither =
      intervalQueryStringBinder.bind("", Map("fromDate" -> Seq("2013-03-30"), "toDate" -> Seq("2014-12-31")))
    maybeEither shouldBe Some(Left("fromDate earlier than 31st March 2013"))
  }

  it should "unbind intervals to query parameters" in {
    val interval = toInterval("2020-01-31", "2020-12-31")
    intervalQueryStringBinder.unbind("", interval) shouldBe "fromDate=2020-01-31&toDate=2020-12-31"
  }
}
