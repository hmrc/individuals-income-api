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

package uk.gov.hmrc.individualsincomeapi.domain.integrationframework.sa

case class SaReturnType(
  utr: Option[String],
  caseStartDate: Option[String],
  receivedDate: Option[String],
  businessDescription: Option[String],
  telephoneNumber: Option[String],
  busStartDate: Option[String],
  busEndDate: Option[String],
  totalTaxPaid: Option[Double],
  totalNIC: Option[Double],
  turnover: Option[Double],
  otherBusinessIncome: Option[Double],
  tradingIncomeAllowance: Option[Double],
  address: Option[Address],
  income: Option[SaIncome]
)
