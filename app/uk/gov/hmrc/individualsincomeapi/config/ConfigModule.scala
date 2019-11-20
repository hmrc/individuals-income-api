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

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.Mode.Mode
import play.api.{Application, Configuration, Environment, Play}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}

class ConfigModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    val delay = configuration.getInt("retryDelay").getOrElse(1000)

    bindConstant().annotatedWith(Names.named("retryDelay")).to(delay)

    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
  }
}

trait ConfigSupport {
  private def current: Application = Play.current

  def config: Configuration = current.configuration
  def mode: Mode = current.mode

  def runModeConfiguration: Configuration = config
  def appNameConfiguration: Configuration = config
  def actorSystem: ActorSystem = current.actorSystem
}
