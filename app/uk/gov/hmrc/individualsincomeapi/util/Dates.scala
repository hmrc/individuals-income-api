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

package uk.gov.hmrc.individualsincomeapi.util

import org.joda.time.{DateTime, Interval, LocalDate}
import uk.gov.hmrc.individualsincomeapi.domain.ValidationException

object Dates {

  val localDatePattern = "yyyy-MM-dd"

  private val desDataInceptionDate = LocalDate.parse("2013-03-31")

  def toFormattedLocalDate(date: DateTime) = date.toLocalDate.toString(localDatePattern)

  def toInterval(fromDate: LocalDate, toDate: LocalDate): Interval = {
    if (fromDate.isBefore(desDataInceptionDate))
      throw new ValidationException("fromDate earlier than 31st March 2013")
    else new Interval(fromDate.toDate.getTime, toDate.toDateTimeAtStartOfDay.plusMillis(1).toDate.getTime)
  }

}