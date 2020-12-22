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

package unit.uk.gov.hmrc.individualsincomeapi.controllers.v2

import play.api.Configuration

trait ScopesConfigHelper {

  val mockScopesConfig = Configuration(
    (s"api-config.scopes.test-scope.fields", List("A", "B", "C")),
    (s"api-config.endpoints.incomePaye.endpoint", "/individuals/income/paye?matchId=<matchId>{&startDate,endDate}"),
    (s"api-config.endpoints.incomePaye.title", "Get an individual's income paye data"),
    (s"api-config.endpoints.incomePaye.fields.A", "foo/bar/one"),
    (s"api-config.endpoints.incomePaye.fields.B", "foo/bar/one"),
    (s"api-config.endpoints.incomePaye.fields.C", "foo/bar/one"),
  )
}
