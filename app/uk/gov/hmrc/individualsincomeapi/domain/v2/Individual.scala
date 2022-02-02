/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.domain.v2

import java.util.UUID

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.{IfPayeEntry, IfSaEntry}

case class MatchedCitizen(matchId: UUID, nino: Nino)

case class Individual(
  matchId: UUID,
  nino: String,
  firstName: String,
  lastName: String,
  dateOfBirth: LocalDate,
  income: Seq[IfPayeEntry],
  saIncome: Seq[IfSaEntry])
