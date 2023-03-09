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

package unit.uk.gov.hmrc.individualsincomeapi.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.gov.hmrc.individualsincomeapi.util.MatchUuidQueryStringBinder

import java.util.UUID.{fromString => uuid}

class MatchUuidQueryStringBinderSpec extends AnyFlatSpec with Matchers with TableDrivenPropertyChecks {

  private val queryStringParameterName = "matchId"

  private val matchUuidQueryStringBinder = new MatchUuidQueryStringBinder

  "Match UUID query string binder" should "fail to bind missing or malformed uuid parameter" in {
    val fixtures = Table(
      ("parameters", "response"),
      (Map[String, Seq[String]]().empty, s"$queryStringParameterName is required"),
      (Map(queryStringParameterName -> Seq.empty[String]), s"$queryStringParameterName is required"),
      (Map(queryStringParameterName -> Seq("")), s"$queryStringParameterName format is invalid"),
      (Map(queryStringParameterName -> Seq("20200131")), s"$queryStringParameterName format is invalid")
    )
    fixtures foreach {
      case (parameters, response) =>
        matchUuidQueryStringBinder.bind("", parameters) shouldBe Some(Left(response))
    }
  }

  it should "bind well formed uuid strings" in {
    val fixtures = Table(
      ("parameters", "response"),
      (
        Map(queryStringParameterName -> Seq("a7b7945e-3ba8-4334-a9cd-2348f98d6867")),
        uuid("a7b7945e-3ba8-4334-a9cd-2348f98d6867")),
      (
        Map(queryStringParameterName -> Seq("b5a9afcb-c7ec-4343-b275-9a3ca0a8f362")),
        uuid("b5a9afcb-c7ec-4343-b275-9a3ca0a8f362")),
      (
        Map(queryStringParameterName -> Seq("c44ca64d-2451-4449-9a9a-70e099efe279")),
        uuid("c44ca64d-2451-4449-9a9a-70e099efe279"))
    )
    fixtures foreach {
      case (parameters, response) =>
        matchUuidQueryStringBinder.bind("", parameters) shouldBe Some(Right(response))
    }
  }

  it should "unbind uuid strings to query parameters" in {
    val fixtures = Table(
      ("parameters", "response"),
      (uuid("a7b7945e-3ba8-4334-a9cd-2348f98d6867"), s"$queryStringParameterName=a7b7945e-3ba8-4334-a9cd-2348f98d6867"),
      (uuid("b5a9afcb-c7ec-4343-b275-9a3ca0a8f362"), s"$queryStringParameterName=b5a9afcb-c7ec-4343-b275-9a3ca0a8f362"),
      (uuid("c44ca64d-2451-4449-9a9a-70e099efe279"), s"$queryStringParameterName=c44ca64d-2451-4449-9a9a-70e099efe279")
    )
    fixtures foreach {
      case (parameters, response) =>
        matchUuidQueryStringBinder.unbind(queryStringParameterName, parameters) shouldBe response
    }
  }

}
