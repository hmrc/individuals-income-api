/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.individualsincomeapi.config

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppConfig @Inject()(runMode: RunMode, servicesConfig: ServicesConfig) {

  def baseUrl(serviceName: String) = servicesConfig.baseUrl(serviceName)

  lazy val desBearerToken = servicesConfig.getString("microservice.services.des.authorization-token")
  lazy val desEnvironment = servicesConfig.getString("microservice.services.des.environment")

}
