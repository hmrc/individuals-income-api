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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework.paye

case class EmployeeNics(
  inPayPeriod1: Option[Double],
  inPayPeriod2: Option[Double],
  inPayPeriod3: Option[Double],
  inPayPeriod4: Option[Double],
  ytd1: Option[Double],
  ytd2: Option[Double],
  ytd3: Option[Double],
  ytd4: Option[Double]
)
