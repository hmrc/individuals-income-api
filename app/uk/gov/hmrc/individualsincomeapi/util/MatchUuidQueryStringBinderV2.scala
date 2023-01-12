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

package uk.gov.hmrc.individualsincomeapi.util

import play.api.mvc.QueryStringBindable

import scala.util.Try

class MatchUuidQueryStringBinderV2 extends QueryStringBindable[String] {

  override def bind(key: String, params: Map[String, Seq[String]]) =
    Option(Try(params.get(key) flatMap (_.headOption) match {
      case Some(parameterValue) => Right(parameterValue)
      case None                 => Left(s"$key is required")
    }) getOrElse Left(s"$key format is invalid"))

  override def unbind(key: String, uuid: String) = s"$key=${uuid}"

}