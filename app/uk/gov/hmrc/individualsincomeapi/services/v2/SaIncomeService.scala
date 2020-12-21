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

package uk.gov.hmrc.individualsincomeapi.services.v2

import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.connector.{IfConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.{MatchNotFoundException, TaxYearInterval}
import uk.gov.hmrc.individualsincomeapi.domain.integrationframework.IfSaEntry
import uk.gov.hmrc.individualsincomeapi.domain.v2.SaFootprint
import uk.gov.hmrc.individualsincomeapi.domain.v2.sandbox.SandboxIncomeData.findByMatchId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait SaIncomeService {

  def endpoints =
    List(
      "incomeSa",
      "incomeSaSummary",
      "incomeSaTrusts",
      "incomeSaForeign",
      "incomeSaPartnerships",
      "incomeSaInterestsAndDividends",
      "incomeSaPensionsAndStateBenefits",
      "incomeSaUkProperties",
      "incomeSaAdditionalInformation",
      "incomeSaOther",
      "incomeSaSource",
      "incomeSaEmployments",
      "incomeSaSelfEmployments",
      "incomeSaFurtherDetails"
    )

  def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFootprint]
}

@Singleton
class LiveSaIncomeService @Inject()(
  matchingConnector: IndividualsMatchingApiConnector,
  ifConnector: IfConnector,
  cache: SaIncomeCacheService,
  scopeService: ScopesService,
  scopesHelper: ScopesHelper,
  @Named("retryDelay") retryDelay: Int)
    extends SaIncomeService {

  private def fetchSaIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[IfSaEntry]] =
    for {
      ninoMatch <- matchingConnector.resolve(matchId)
      saIncome <- cache.get(
                   SaCacheId(
                     matchId,
                     taxYearInterval,
                     scopeService.getValidFieldsForCacheKey(scopes.toList, endpoints)),
                   withRetry(
                     ifConnector.fetchSelfAssessmentIncome(
                       ninoMatch.nino,
                       taxYearInterval,
                       Option(scopesHelper.getQueryStringFor(scopes.toList, endpoints)).filter(_.nonEmpty)
                     )
                   )
                 )
    } yield saIncome

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFootprint] =
    for {
      ninoMatch   <- matchingConnector.resolve(matchId)
      ifSaEntries <- fetchSaIncome(ninoMatch.matchId, taxYearInterval, scopes)
    } yield SaFootprint(ifSaEntries)

  private def withRetry[T](body: => Future[T]): Future[T] = body recoverWith {
    case Upstream5xxResponse(_, 503, 503, _) => Thread.sleep(retryDelay); body
  }

}

@Singleton
class SandboxSaIncomeService extends SaIncomeService {

  private def fetchSaIncome(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[Seq[IfSaEntry]] =
    findByMatchId(matchId).map(_.saIncome) match {
      case Some(saIncome) => successful(saIncome)
      case None           => failed(new MatchNotFoundException)
    }

  override def fetchSaFootprint(matchId: UUID, taxYearInterval: TaxYearInterval, scopes: Iterable[String])(
    implicit hc: HeaderCarrier): Future[SaFootprint] =
    fetchSaIncome(matchId, taxYearInterval, scopes).map(saFootprint => SaFootprint(saFootprint))

}
