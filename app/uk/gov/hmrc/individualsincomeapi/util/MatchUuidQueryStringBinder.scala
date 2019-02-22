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

package uk.gov.hmrc.individualsincomeapi.util

import java.util.UUID

import scala.util.Try

class MatchUuidQueryStringBinder extends AbstractQueryStringBindable[UUID] {

  private val parameterName = "matchId"

  override def bind(key: String, params: Map[String, Seq[String]]) = {
    Option(Try(params.get(parameterName) flatMap (_.headOption) match {
      case Some(parameterValue) => Right(UUID.fromString(parameterValue))
      case None => Left(errorResponse(s"$parameterName is required"))
    }) getOrElse Left(errorResponse(s"$parameterName format is invalid")))
  }

  override def unbind(key: String, uuid: UUID) = s"$key=${uuid.toString}"

}
