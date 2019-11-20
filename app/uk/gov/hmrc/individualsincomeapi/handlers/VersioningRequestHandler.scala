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

package uk.gov.hmrc.individualsincomeapi.handlers

import javax.inject.Inject
import play.api.http.{HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import uk.gov.hmrc.individualsincomeapi.config.ConfigSupport
import uk.gov.hmrc.play.bootstrap.http.RequestHandler
import uk.gov.hmrc.individualsincomeapi.play.RequestHeaderUtils._

class VersioningRequestHandler @Inject()(
                                          router: Router,
                                          errorHandler: HttpErrorHandler,
                                          configuration: HttpConfiguration,
                                          filters: HttpFilters)
  extends RequestHandler(router, errorHandler, configuration, filters)
    with ConfigSupport {

  private lazy val unversionedContexts = config
    .getStringSeq("versioning.unversionedContexts")
    .getOrElse(Seq.empty[String])

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    val requestContext = extractUriContext(request)
    if (unversionedContexts.contains(requestContext)) {
      super.routeRequest(request)
    } else {
      super.routeRequest(getVersionedRequest(request))
    }

  }
}
