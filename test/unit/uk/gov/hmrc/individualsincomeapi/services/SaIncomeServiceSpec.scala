/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.uk.gov.hmrc.individualsincomeapi.services

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.mockito.Matchers.{any, refEq}
import org.mockito.Mockito.{verify, times}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.cache.{CacheConfiguration, SaCacheId, SaIncomeCacheService, ShortLivedCache}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.{LiveSaIncomeService, SandboxSaIncomeService}
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.individualsincomeapi.util.Dates

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class SaIncomeServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures with Dates {

  val utr = SaUtr("2432552644")
  val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")
  val liveMatchId = UUID.randomUUID()
  val liveNino = Nino("AA100009B")
  val taxYearInterval = TaxYearInterval(TaxYear("2013-14"), TaxYear("2016-17"))
  val saCacheId = SaCacheId(liveNino, taxYearInterval)
  val desIncomes = Seq(
    DesSAIncome(
      taxYear = "2015",
      returnList = Seq(
      DesSAReturn(
        caseStartDate = LocalDate.parse("2011-06-06"),
        receivedDate = LocalDate.parse("2015-10-06"),
        utr = utr,
        income = SAIncome(
          incomeFromAllEmployments = None,
          profitFromSelfEmployment = None,
          incomeFromSelfAssessment = Some(35000.55),
          incomeFromTrust = Some(2600.55),
          incomeFromForeign4Sources = Some(500.55),
          profitFromPartnerships = Some(555.55),
          incomeFromUkInterest = Some(43.56),
          incomeFromForeignDividends = Some(72.57),
          incomeFromInterestNDividendsFromUKCompaniesNTrusts = Some(16.32),
          incomeFromProperty = Some(1276.67),
          incomeFromPensions = Some(52.56),
          incomeFromGainsOnLifePolicies = Some(45.20),
          incomeFromSharesOptions = Some(12.45),
          incomeFromOther = Some(134.56)
        )
      ))),
    DesSAIncome(
      taxYear = "2016",
      returnList = Seq(
        DesSAReturn(
          caseStartDate = LocalDate.parse("2011-06-06"),
          receivedDate = LocalDate.parse("2016-06-06"),
          utr = utr,
          income = SAIncome(
            incomeFromAllEmployments = Some(1555.55),
            profitFromSelfEmployment = Some(2500.55),
            incomeFromSelfAssessment = None,
            incomeFromTrust = None,
            incomeFromForeign4Sources = None,
            incomeFromUkInterest = None,
            incomeFromForeignDividends = None,
            incomeFromInterestNDividendsFromUKCompaniesNTrusts = None,
            incomeFromProperty = None,
            incomeFromPensions = None,
            incomeFromGainsOnLifePolicies = None,
            incomeFromSharesOptions = None
          )
        )))
  )

  trait Setup {
    implicit val hc = HeaderCarrier()
    val matchingConnector = mock[IndividualsMatchingApiConnector]
    val desConnector = mock[DesConnector]
    val shortLivedCache = mock[ShortLivedCache]
    val configuration = mock[CacheConfiguration]
    val saIncomeCacheService = new SaIncomeCacheService(shortLivedCache, configuration)
    val sandboxSaIncomeService = new SandboxSaIncomeService()
    val liveSaIncomeService = new LiveSaIncomeService(matchingConnector, desConnector, saIncomeCacheService, 1)
  }

  "LiveIncomeService.fetchSaFootprintByMatchId" should {
    "return the saFootprint with saReturns sorted by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaFootprintByMatchId(liveMatchId, taxYearInterval))

      result shouldBe SaFootprint(
        registrations = Seq(SaRegistration(utr, LocalDate.parse("2011-06-06"))),
        taxReturns = Seq(
          SaTaxReturn(TaxYear("2015-16"), Seq(SaSubmission(utr, LocalDate.parse("2016-06-06")))),
          SaTaxReturn(TaxYear("2014-15"), Seq(SaSubmission(utr, LocalDate.parse("2015-10-06"))))
      ))
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaFootprintByMatchId(liveMatchId, taxYearInterval))
      }
    }

    "retry the SA lookup once if DES returns a 503" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval))
        .willReturn(Future.failed(Upstream5xxResponse("""¯\_(ツ)_/¯""", 503, 503)))
        .willReturn(desIncomes)

      await(liveSaIncomeService.fetchSaFootprintByMatchId(liveMatchId, taxYearInterval)) shouldBe SaFootprint(
        registrations = Seq(SaRegistration(utr, LocalDate.parse("2011-06-06"))),
        taxReturns = Seq(
          SaTaxReturn(TaxYear("2015-16"), Seq(SaSubmission(utr, LocalDate.parse("2016-06-06")))),
          SaTaxReturn(TaxYear("2014-15"), Seq(SaSubmission(utr, LocalDate.parse("2015-10-06"))))
        )
      )

      verify(desConnector, times(2)).fetchSelfAssessmentIncome(any(), any())(any(), any())
    }
  }

  "SandboxSaIncomeService.fetchSaReturnsByMatchId" should {
    "return the saFootprint with saReturns sorted by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaFootprintByMatchId(sandboxMatchId, taxYearInterval))

      result shouldBe SaFootprint(
        registrations = Seq(SaRegistration(SaUtr("2432552635"), LocalDate.parse("2012-01-06"))),
        taxReturns = Seq(
          SaTaxReturn(TaxYear("2014-15"), Seq(SaSubmission(SaUtr("2432552635"), LocalDate.parse("2015-10-06")))),
          SaTaxReturn(TaxYear("2013-14"), Seq(SaSubmission(SaUtr("2432552635"), LocalDate.parse("2014-06-06"))))
      ))
    }

    "filter out returns not in the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaFootprintByMatchId(sandboxMatchId, taxYearInterval.copy(toTaxYear = TaxYear("2013-14"))))

      result.taxReturns shouldBe Seq(SaTaxReturn(TaxYear("2013-14"), Seq(SaSubmission(SaUtr("2432552635"), LocalDate.parse("2014-06-06")))))
    }

    "return an empty footprint when no annual return exists for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaFootprintByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))

      result shouldBe SaFootprint(Seq.empty, Seq.empty)
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaFootprintByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchEmploymentsIncomeByMatchId" should {
    "return the employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchEmploymentsIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualEmployments(TaxYear("2015-16"), Seq(SaEmploymentsIncome(utr, 1555.55))),
        SaAnnualEmployments(TaxYear("2014-15"), Seq(SaEmploymentsIncome(utr, 0.0)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchEmploymentsIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchEmploymentsIncomeByMatchId" should {
    "return the employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))

      result shouldBe Seq(
        SaAnnualEmployments(TaxYear("2014-15"), Seq(SaEmploymentsIncome(sandboxUtr, 0))),
        SaAnnualEmployments(TaxYear("2013-14"), Seq(SaEmploymentsIncome(sandboxUtr, 5000)))
      )
    }

    "filter out employments income not in the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(sandboxMatchId, taxYearInterval.copy(fromTaxYear = TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualEmployments(TaxYear("2014-15"), Seq(SaEmploymentsIncome(sandboxUtr, 0)))
      )
    }

    "return an empty list when no employments income exists for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))

      result shouldBe Seq()
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchEmploymentsIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSelfEmploymentsIncomeByMatchId" should {
    "return the self employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualSelfEmployments(TaxYear("2015-16"), Seq(SaSelfEmploymentsIncome(utr, 2500.55))),
        SaAnnualSelfEmployments(TaxYear("2014-15"), Seq(SaSelfEmploymentsIncome(utr, 0.0)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId" should {
    "return the self employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualSelfEmployments(TaxYear("2014-15"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 0.0))),
        SaAnnualSelfEmployments(TaxYear("2013-14"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 10500)))
      )
    }

    "return an empty list when no self employments exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaReturnsSummaryByMatchId" should {
    "return sa tax return summaries by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaReturnsSummaryByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaTaxReturnSummaries(TaxYear("2015-16"), Seq(SaTaxReturnSummary(utr, 0.0))),
        SaTaxReturnSummaries(TaxYear("2014-15"), Seq(SaTaxReturnSummary(utr, 35000.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaReturnsSummaryByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaReturnsSummaryByMatchId" should {
    "return the sa tax return summaries by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaTaxReturnSummaries(TaxYear("2014-15"), Seq(SaTaxReturnSummary(sandboxUtr, 0.0))),
        SaTaxReturnSummaries(TaxYear("2013-14"), Seq(SaTaxReturnSummary(sandboxUtr, 30000)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaTrustsByMatchId" should {
    "return sa tax return trusts income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaTrustsIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualTrustIncomes(TaxYear("2015-16"), Seq(SaAnnualTrustIncome(utr, 0.0))),
        SaAnnualTrustIncomes(TaxYear("2014-15"), Seq(SaAnnualTrustIncome(utr, 2600.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaTrustsIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaTrustsByMatchId" should {
    "return the sa trusts by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaTrustsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualTrustIncomes(TaxYear("2014-15"), Seq(SaAnnualTrustIncome(sandboxUtr, 0))),
        SaAnnualTrustIncomes(TaxYear("2013-14"), Seq(SaAnnualTrustIncome(sandboxUtr, 2143.32)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaReturnsSummaryByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaTrustsIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaForeignIncomeByMatchId" should {
    "return sa tax return foreign income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaForeignIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualForeignIncomes(TaxYear("2015-16"), Seq(SaAnnualForeignIncome(utr, 0.0))),
        SaAnnualForeignIncomes(TaxYear("2014-15"), Seq(SaAnnualForeignIncome(utr, 500.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaForeignIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaForeignIncomeByMatchId" should {
    "return the sa foreign income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaForeignIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualForeignIncomes(TaxYear("2014-15"), Seq(SaAnnualForeignIncome(sandboxUtr, 0))),
        SaAnnualForeignIncomes(TaxYear("2013-14"), Seq(SaAnnualForeignIncome(sandboxUtr, 1054.65)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaForeignIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaForeignIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaPartnershipsIncomeByMatchId" should {
    "return sa tax return partnership income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaPartnershipsIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualPartnershipIncomes(TaxYear("2015-16"), Seq(SaAnnualPartnershipIncome(utr, 0.0))),
        SaAnnualPartnershipIncomes(TaxYear("2014-15"), Seq(SaAnnualPartnershipIncome(utr, 555.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaPartnershipsIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaPartnershipsIncomeByMatchId" should {
    "return the sa partnership income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaPartnershipsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualPartnershipIncomes(TaxYear("2014-15"), Seq(SaAnnualPartnershipIncome(sandboxUtr, 0))),
        SaAnnualPartnershipIncomes(TaxYear("2013-14"), Seq(SaAnnualPartnershipIncome(sandboxUtr, 324.54)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaPartnershipsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaPartnershipsIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId" should {
    "return sa tax return interests and dividends income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualInterestAndDividendIncomes(TaxYear("2015-16"), Seq(SaAnnualInterestAndDividendIncome(utr, 0.0, 0.0, 0.0))),
        SaAnnualInterestAndDividendIncomes(TaxYear("2014-15"), Seq(SaAnnualInterestAndDividendIncome(utr, 43.56, 72.57, 16.32)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId" should {
    "return the sa interests and dividends income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualInterestAndDividendIncomes(TaxYear("2014-15"), Seq(SaAnnualInterestAndDividendIncome(sandboxUtr, 0, 0, 0))),
        SaAnnualInterestAndDividendIncomes(TaxYear("2013-14"), Seq(SaAnnualInterestAndDividendIncome(sandboxUtr, 12.46, 25.86, 657.89)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaUkPropertiesIncomeByMatchId" should {
    "return sa UK properties income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaUkPropertiesIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualUkPropertiesIncomes(TaxYear("2015-16"), Seq(SaAnnualUkPropertiesIncome(utr, 0.0))),
        SaAnnualUkPropertiesIncomes(TaxYear("2014-15"), Seq(SaAnnualUkPropertiesIncome(utr, 1276.67)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaUkPropertiesIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaUkPropertiesIncomeByMatchId" should {
    "return the sa UK properties income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaUkPropertiesIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualUkPropertiesIncomes(TaxYear("2014-15"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 0))),
        SaAnnualUkPropertiesIncomes(TaxYear("2013-14"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 1276.67)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaUkPropertiesIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaUkPropertiesIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId" should {
    "return sa tax return pensions and state benefits income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualPensionAndStateBenefitIncomes(TaxYear("2015-16"), Seq(SaAnnualPensionAndStateBenefitIncome(utr, 0.0))),
        SaAnnualPensionAndStateBenefitIncomes(TaxYear("2014-15"), Seq(SaAnnualPensionAndStateBenefitIncome(utr, 52.56)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId" should {
    "return the sa pensions and state benefits income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualPensionAndStateBenefitIncomes(TaxYear("2014-15"), Seq(SaAnnualPensionAndStateBenefitIncome(sandboxUtr, 0))),
        SaAnnualPensionAndStateBenefitIncomes(TaxYear("2013-14"), Seq(SaAnnualPensionAndStateBenefitIncome(sandboxUtr, 52.79)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaAdditionalInformationByMatchId" should {
    "return sa tax return additional information by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaAdditionalInformationByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualAdditionalInformations(TaxYear("2015-16"), Seq(SaAnnualAdditionalInformation(utr, 0.0, 0.0))),
        SaAnnualAdditionalInformations(TaxYear("2014-15"), Seq(SaAnnualAdditionalInformation(utr, 45.20, 12.45)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaAdditionalInformationByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaAdditionalInformationByMatchId" should {
    "return the sa additional information by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaAdditionalInformationByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualAdditionalInformations(TaxYear("2014-15"), Seq(SaAnnualAdditionalInformation(sandboxUtr, 0, 0))),
        SaAnnualAdditionalInformations(TaxYear("2013-14"), Seq(SaAnnualAdditionalInformation(sandboxUtr, 44.54, 52.34)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaAdditionalInformationByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaAdditionalInformationByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }

  "LiveIncomeService.fetchSaOtherIncomeByMatchId" should {
    "return sa tax return other income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(MatchedCitizen(liveMatchId, liveNino))
      given(shortLivedCache.fetch[Seq[DesSAIncome]](refEq(saCacheId.id), refEq(saIncomeCacheService.key))(any())).willReturn(successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(desIncomes)

      val result = await(liveSaIncomeService.fetchSaOtherIncomeByMatchId(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualOtherIncomes(TaxYear("2015-16"), Seq(SaAnnualOtherIncome(utr, 0.0))),
        SaAnnualOtherIncomes(TaxYear("2014-15"), Seq(SaAnnualOtherIncome(utr, 134.56)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaOtherIncomeByMatchId(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaOtherIncomeByMatchId" should {
    "return the sa other income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaOtherIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2013-14"), TaxYear("2014-15"))))

      result shouldBe Seq(
        SaAnnualOtherIncomes(TaxYear("2014-15"), Seq(SaAnnualOtherIncome(sandboxUtr, 0))),
        SaAnnualOtherIncomes(TaxYear("2013-14"), Seq(SaAnnualOtherIncome(sandboxUtr, 26.70)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaOtherIncomeByMatchId(sandboxMatchId, TaxYearInterval(TaxYear("2015-16"), TaxYear("2015-16"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService.fetchSaOtherIncomeByMatchId(UUID.randomUUID(), TaxYearInterval(TaxYear("2013-14"), TaxYear("2015-16"))))
      }
    }
  }
}