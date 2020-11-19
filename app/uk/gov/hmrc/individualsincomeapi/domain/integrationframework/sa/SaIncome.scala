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

case class SaIncome(
  selfAssessment: Option[Double],
  allEmployments: Option[Double],
  ukInterest: Option[Double],
  foreignDivs: Option[Double],
  ukDivsAndInterest: Option[Double],
  partnerships: Option[Double],
  pensions: Option[Double],
  selfEmployment: Option[Double],
  trusts: Option[Double],
  ukProperty: Option[Double],
  foreign: Option[Double],
  lifePolicies: Option[Double],
  shares: Option[Double],
  other: Option[Double]
)
