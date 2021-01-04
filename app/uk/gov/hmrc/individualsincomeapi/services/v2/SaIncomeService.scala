/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.services.v2

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}

trait SaIncomeService {

  // TODO - to implement when we wire up the endpoints (See V1 for reference)

}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

  // TODO - to implement when we wire up the endpoints (See V1 for reference)

}

@Singleton
class LiveSaIncomeService @Inject()(
  matchingConnector: IndividualsMatchingApiConnector,
  desConnector: DesConnector, // TODO - replace with IfConnector
  saIncomeCacheService: SaIncomeCacheService,
  @Named("retryDelay") retryDelay: Int)
    extends SaIncomeService {

  // TODO - to implement when we wire up the endpoints (See V1 for reference)

}
