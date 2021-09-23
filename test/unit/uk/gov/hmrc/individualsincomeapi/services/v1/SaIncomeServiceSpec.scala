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

package unit.uk.gov.hmrc.individualsincomeapi.services.v1

import java.util.UUID

import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.individualsincomeapi.cache.v1.{CacheRepositoryConfiguration, ShortLivedCache}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.v1.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain.{v1, _}
import uk.gov.hmrc.individualsincomeapi.domain.des.{DesAddress, DesSAIncome, DesSAReturn, SAIncome}
import uk.gov.hmrc.individualsincomeapi.domain.v1.{MatchedCitizen, SaAnnualAdditionalInformation, SaAnnualAdditionalInformations, SaAnnualEmployments, SaAnnualForeignIncome, SaAnnualForeignIncomes, SaAnnualInterestAndDividendIncome, SaAnnualInterestAndDividendIncomes, SaAnnualOtherIncome, SaAnnualOtherIncomes, SaAnnualPartnershipIncome, SaAnnualPartnershipIncomes, SaAnnualPensionAndStateBenefitIncome, SaAnnualPensionAndStateBenefitIncomes, SaAnnualSelfEmployments, SaAnnualTrustIncome, SaAnnualTrustIncomes, SaAnnualUkPropertiesIncome, SaAnnualUkPropertiesIncomes, SaEmploymentsIncome, SaFootprint, SaIncomeSource, SaIncomeSources, SaRegistration, SaSelfEmploymentsIncome, SaSubmission, SaTaxReturn, SaTaxReturnSummaries, SaTaxReturnSummary, SourceAddress}
import uk.gov.hmrc.individualsincomeapi.services.v1.{LiveSaIncomeService, SaCacheId, CacheService, SandboxSaIncomeService}
import unit.uk.gov.hmrc.individualsincomeapi.util.TestDates
import utils.TestSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class SaIncomeServiceSpec extends TestSupport with MockitoSugar with ScalaFutures with TestDates {

  "LiveIncomeService.fetchSaFootprintByMatchId" should {
    "return the saFootprint with saReturns sorted by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchSaFootprint(liveMatchId, taxYearInterval))

      result shouldBe SaFootprint(
        registrations = Seq(SaRegistration(utr, Some(LocalDate.parse("2017-06-06")))),
        taxReturns = Seq(
          SaTaxReturn(TaxYear("2018-19"), Seq(SaSubmission(utr, Some(LocalDate.parse("2019-06-06"))))),
          v1.SaTaxReturn(TaxYear("2017-18"), Seq(SaSubmission(utr, Some(LocalDate.parse("2018-10-06")))))
        )
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaFootprint(liveMatchId, taxYearInterval))
      }
    }

    "retry the SA lookup once if DES returns a 503" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval))
        .willReturn(Future.failed(Upstream5xxResponse("""¯\_(ツ)_/¯""", 503, 503)))
        .willReturn(successful(desIncomes))

      await(liveSaIncomeService.fetchSaFootprint(liveMatchId, taxYearInterval)) shouldBe SaFootprint(
        registrations = Seq(SaRegistration(utr, Some(LocalDate.parse("2017-06-06")))),
        taxReturns = Seq(
          v1.SaTaxReturn(TaxYear("2018-19"), Seq(SaSubmission(utr, Some(LocalDate.parse("2019-06-06"))))),
          v1.SaTaxReturn(TaxYear("2017-18"), Seq(SaSubmission(utr, Some(LocalDate.parse("2018-10-06")))))
        )
      )

      verify(desConnector, times(2)).fetchSelfAssessmentIncome(any(), any())(any(), any())
    }
  }

  "SandboxSaIncomeService.fetchSaReturnsByMatchId" should {
    "return the saFootprint with saReturns sorted by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(sandboxSaIncomeService.fetchSaFootprint(sandboxMatchId, taxYearInterval))

      result shouldBe SaFootprint(
        registrations = Seq(SaRegistration(SaUtr("2432552635"), Some(LocalDate.parse("2015-01-06")))),
        taxReturns = Seq(
          v1.SaTaxReturn(
            TaxYear("2017-18"),
            Seq(SaSubmission(SaUtr("2432552635"), Some(LocalDate.parse("2018-10-06"))))),
          v1.SaTaxReturn(
            TaxYear("2016-17"),
            Seq(SaSubmission(SaUtr("2432552635"), Some(LocalDate.parse("2017-06-06")))))
        )
      )
    }

    "filter out returns not in the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService.fetchSaFootprint(sandboxMatchId, taxYearInterval.copy(toTaxYear = TaxYear("2016-17"))))

      result.taxReturns shouldBe Seq(
        v1.SaTaxReturn(TaxYear("2016-17"), Seq(SaSubmission(SaUtr("2432552635"), Some(LocalDate.parse("2017-06-06"))))))
    }

    "return an empty footprint when no annual return exists for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchSaFootprint(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))

      result shouldBe SaFootprint(Seq.empty, Seq.empty)
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchSaFootprint(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchEmploymentsIncomeByMatchId" should {
    "return the employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchEmploymentsIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualEmployments(TaxYear("2018-19"), Seq(SaEmploymentsIncome(utr, 1555.55))),
        v1.SaAnnualEmployments(TaxYear("2017-18"), Seq(SaEmploymentsIncome(utr, 0.0)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchEmploymentsIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchEmploymentsIncomeByMatchId" should {
    "return the employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchEmploymentsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))

      result shouldBe Seq(
        v1.SaAnnualEmployments(TaxYear("2017-18"), Seq(SaEmploymentsIncome(sandboxUtr, 0))),
        v1.SaAnnualEmployments(TaxYear("2016-17"), Seq(SaEmploymentsIncome(sandboxUtr, 5000)))
      )
    }

    "filter out employments income not in the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchEmploymentsIncome(sandboxMatchId, taxYearInterval.copy(fromTaxYear = TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualEmployments(TaxYear("2017-18"), Seq(SaEmploymentsIncome(sandboxUtr, 0)))
      )
    }

    "return an empty list when no employments income exists for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchEmploymentsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))

      result shouldBe Seq()
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchEmploymentsIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSelfEmploymentsIncomeByMatchId" should {
    "return the self employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchSelfEmploymentsIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualSelfEmployments(TaxYear("2018-19"), Seq(SaSelfEmploymentsIncome(utr, 2500.55))),
        v1.SaAnnualSelfEmployments(TaxYear("2017-18"), Seq(SaSelfEmploymentsIncome(utr, 0.0)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSelfEmploymentsIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSelfEmploymentsIncomeByMatchId" should {
    "return the self employments income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchSelfEmploymentsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualSelfEmployments(TaxYear("2017-18"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 0.0))),
        v1.SaAnnualSelfEmployments(TaxYear("2016-17"), Seq(SaSelfEmploymentsIncome(sandboxUtr, 10500)))
      )
    }

    "return an empty list when no self employments exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchSelfEmploymentsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchSelfEmploymentsIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaReturnsSummaryByMatchId" should {
    "return sa tax return summaries by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchReturnsSummary(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaTaxReturnSummaries(TaxYear("2018-19"), Seq(SaTaxReturnSummary(utr, 0.0))),
        v1.SaTaxReturnSummaries(TaxYear("2017-18"), Seq(SaTaxReturnSummary(utr, 35000.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchReturnsSummary(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaReturnsSummaryByMatchId" should {
    "return the sa tax return summaries by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchReturnsSummary(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaTaxReturnSummaries(TaxYear("2017-18"), Seq(SaTaxReturnSummary(sandboxUtr, 0.0))),
        v1.SaTaxReturnSummaries(TaxYear("2016-17"), Seq(SaTaxReturnSummary(sandboxUtr, 30000)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchReturnsSummary(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchReturnsSummary(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaTrustsByMatchId" should {
    "return sa tax return trusts income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchTrustsIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualTrustIncomes(TaxYear("2018-19"), Seq(SaAnnualTrustIncome(utr, 0.0))),
        v1.SaAnnualTrustIncomes(TaxYear("2017-18"), Seq(SaAnnualTrustIncome(utr, 2600.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchTrustsIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaTrustsByMatchId" should {
    "return the sa trusts by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchTrustsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualTrustIncomes(TaxYear("2017-18"), Seq(SaAnnualTrustIncome(sandboxUtr, 0))),
        v1.SaAnnualTrustIncomes(TaxYear("2016-17"), Seq(SaAnnualTrustIncome(sandboxUtr, 2143.32)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchReturnsSummary(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchTrustsIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaForeignIncomeByMatchId" should {
    "return sa tax return foreign income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchForeignIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualForeignIncomes(TaxYear("2018-19"), Seq(SaAnnualForeignIncome(utr, 0.0))),
        v1.SaAnnualForeignIncomes(TaxYear("2017-18"), Seq(SaAnnualForeignIncome(utr, 500.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchForeignIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaForeignIncomeByMatchId" should {
    "return the sa foreign income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchForeignIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualForeignIncomes(TaxYear("2017-18"), Seq(SaAnnualForeignIncome(sandboxUtr, 0))),
        v1.SaAnnualForeignIncomes(TaxYear("2016-17"), Seq(SaAnnualForeignIncome(sandboxUtr, 1054.65)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchForeignIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchForeignIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaPartnershipsIncomeByMatchId" should {
    "return sa tax return partnership income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchPartnershipsIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualPartnershipIncomes(TaxYear("2018-19"), Seq(SaAnnualPartnershipIncome(utr, 0.0))),
        v1.SaAnnualPartnershipIncomes(TaxYear("2017-18"), Seq(SaAnnualPartnershipIncome(utr, 555.55)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchPartnershipsIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaPartnershipsIncomeByMatchId" should {
    "return the sa partnership income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchPartnershipsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualPartnershipIncomes(TaxYear("2017-18"), Seq(SaAnnualPartnershipIncome(sandboxUtr, 0))),
        v1.SaAnnualPartnershipIncomes(TaxYear("2016-17"), Seq(SaAnnualPartnershipIncome(sandboxUtr, 324.54)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchPartnershipsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchPartnershipsIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId" should {
    "return sa tax return interests and dividends income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchInterestsAndDividendsIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualInterestAndDividendIncomes(
          TaxYear("2018-19"),
          Seq(SaAnnualInterestAndDividendIncome(utr, 0.0, 0.0, 0.0))),
        v1.SaAnnualInterestAndDividendIncomes(
          TaxYear("2017-18"),
          Seq(SaAnnualInterestAndDividendIncome(utr, 43.56, 72.57, 16.32)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchInterestsAndDividendsIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaInterestsAndDividendsIncomeByMatchId" should {
    "return the sa interests and dividends income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchInterestsAndDividendsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualInterestAndDividendIncomes(
          TaxYear("2017-18"),
          Seq(SaAnnualInterestAndDividendIncome(sandboxUtr, 0, 0, 0))),
        v1.SaAnnualInterestAndDividendIncomes(
          TaxYear("2016-17"),
          Seq(SaAnnualInterestAndDividendIncome(sandboxUtr, 12.46, 25.86, 657.89)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchInterestsAndDividendsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(sandboxSaIncomeService
          .fetchInterestsAndDividendsIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaUkPropertiesIncomeByMatchId" should {
    "return sa UK properties income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchUkPropertiesIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualUkPropertiesIncomes(TaxYear("2018-19"), Seq(SaAnnualUkPropertiesIncome(utr, 0.0))),
        v1.SaAnnualUkPropertiesIncomes(TaxYear("2017-18"), Seq(SaAnnualUkPropertiesIncome(utr, 1276.67)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchUkPropertiesIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaUkPropertiesIncomeByMatchId" should {
    "return the sa UK properties income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchUkPropertiesIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualUkPropertiesIncomes(TaxYear("2017-18"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 0))),
        v1.SaAnnualUkPropertiesIncomes(TaxYear("2016-17"), Seq(SaAnnualUkPropertiesIncome(sandboxUtr, 1276.67)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchUkPropertiesIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchUkPropertiesIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId" should {
    "return sa tax return pensions and state benefits income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchPensionsAndStateBenefitsIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualPensionAndStateBenefitIncomes(TaxYear("2018-19"), Seq(SaAnnualPensionAndStateBenefitIncome(utr, 0.0))),
        v1.SaAnnualPensionAndStateBenefitIncomes(
          TaxYear("2017-18"),
          Seq(SaAnnualPensionAndStateBenefitIncome(utr, 52.56)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchPensionsAndStateBenefitsIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaPensionsAndStateBenefitsIncomeByMatchId" should {
    "return the sa pensions and state benefits income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchPensionsAndStateBenefitsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualPensionAndStateBenefitIncomes(
          TaxYear("2017-18"),
          Seq(SaAnnualPensionAndStateBenefitIncome(sandboxUtr, 0))),
        v1.SaAnnualPensionAndStateBenefitIncomes(
          TaxYear("2016-17"),
          Seq(SaAnnualPensionAndStateBenefitIncome(sandboxUtr, 52.79)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchPensionsAndStateBenefitsIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService.fetchPensionsAndStateBenefitsIncome(
            UUID.randomUUID(),
            TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaAdditionalInformationByMatchId" should {
    "return sa tax return additional information by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchAdditionalInformation(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualAdditionalInformations(TaxYear("2018-19"), Seq(SaAnnualAdditionalInformation(utr, 0.0, 0.0))),
        v1.SaAnnualAdditionalInformations(TaxYear("2017-18"), Seq(SaAnnualAdditionalInformation(utr, 45.20, 12.45)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchAdditionalInformation(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaAdditionalInformationByMatchId" should {
    "return the sa additional information by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchAdditionalInformation(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualAdditionalInformations(TaxYear("2017-18"), Seq(SaAnnualAdditionalInformation(sandboxUtr, 0, 0))),
        v1.SaAnnualAdditionalInformations(
          TaxYear("2016-17"),
          Seq(SaAnnualAdditionalInformation(sandboxUtr, 44.54, 52.34)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchAdditionalInformation(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchAdditionalInformation(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaOtherIncomeByMatchId" should {
    "return sa tax return other income by tax year DESCENDING when the matchId is valid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(desIncomes))

      val result = await(liveSaIncomeService.fetchOtherIncome(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaAnnualOtherIncomes(TaxYear("2018-19"), Seq(SaAnnualOtherIncome(utr, 0.0))),
        v1.SaAnnualOtherIncomes(TaxYear("2017-18"), Seq(SaAnnualOtherIncome(utr, 134.56)))
      )
    }

    "fail with MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchOtherIncome(liveMatchId, taxYearInterval))
      }
    }
  }

  "SandboxSaIncomeService.fetchSaOtherIncomeByMatchId" should {
    "return the sa other income by tax year DESCENDING when the matchId is valid" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchOtherIncome(sandboxMatchId, TaxYearInterval(TaxYear("2016-17"), TaxYear("2017-18"))))

      result shouldBe Seq(
        v1.SaAnnualOtherIncomes(TaxYear("2017-18"), Seq(SaAnnualOtherIncome(sandboxUtr, 0))),
        v1.SaAnnualOtherIncomes(TaxYear("2016-17"), Seq(SaAnnualOtherIncome(sandboxUtr, 26.70)))
      )
    }

    "return an empty list when no sa tax returns exist for the requested period" in new Setup {
      val result = await(
        sandboxSaIncomeService
          .fetchOtherIncome(sandboxMatchId, TaxYearInterval(TaxYear("2018-19"), TaxYear("2018-19"))))
      result shouldBe Seq.empty
    }

    "fail with MatchNotFoundException when the matchId is not the sandbox matchId" in new Setup {
      intercept[MatchNotFoundException] {
        await(
          sandboxSaIncomeService
            .fetchOtherIncome(UUID.randomUUID(), TaxYearInterval(TaxYear("2016-17"), TaxYear("2018-19"))))
      }
    }
  }

  "LiveIncomeService.fetchSaSources" should {
    "return the sa income sources by tax year in descending order when the matchId is valid" in new Setup {
      val income = DesSAIncome(
        "2019",
        Seq(
          anSAReturn.copy(
            businessDescription = Some("business"),
            addressLine1 = Some("line 1"),
            addressLine2 = Some("line 2"),
            addressLine3 = Some("line 3"),
            addressLine4 = Some("line 4"),
            postalCode = Some("AA11 1AA"),
            telephoneNumber = Some("01234567890"),
            baseAddressEffectiveDate = Some(new LocalDate(2018, 9, 7)),
            addressTypeIndicator = Some("B")
          ))
      )

      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(Seq(income)))

      val result = await(liveSaIncomeService.fetchSaIncomeSources(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        SaIncomeSources(
          TaxYear("2018-19"),
          Seq(
            SaIncomeSource(
              anSAReturn.utr,
              Some("business"),
              Some(
                SourceAddress(
                  Some("line 1"),
                  Some("line 2"),
                  Some("line 3"),
                  Some("line 4"),
                  None,
                  Some("AA11 1AA"),
                  Some(new LocalDate(2018, 9, 7)),
                  Some("homeAddress")
                )),
              Some("01234567890")
            )
          )
        ))
    }

    "filter out empty addresses from the response" in new Setup {
      val income = DesSAIncome(
        "2019",
        Seq(
          anSAReturn.copy(
            businessDescription = Some("business"),
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postalCode = None,
            telephoneNumber = Some("01234567890"),
            baseAddressEffectiveDate = None,
            addressTypeIndicator = None
          ))
      )

      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(Seq(income)))

      val result = await(liveSaIncomeService.fetchSaIncomeSources(liveMatchId, taxYearInterval))

      result shouldBe Seq(
        v1.SaIncomeSources(
          TaxYear("2018-19"),
          Seq(
            SaIncomeSource(
              anSAReturn.utr,
              Some("business"),
              None,
              Some("01234567890")
            )
          )))
    }

    "convert addressTypeIndicator to addressType" in new Setup {
      val conversions = Map(
        Some("B")          -> Some("homeAddress"),
        Some("C")          -> Some("correspondenceAddress"),
        Some("thing else") -> Some("other"),
        None               -> None
      )

      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))

      conversions.foreach {
        case (indicator, addressType) =>
          given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval))
            .willReturn(
              successful(
                Seq(
                  DesSAIncome(
                    "2019",
                    Seq(
                      anSAReturn.copy(
                        businessDescription = Some("thing"),
                        addressTypeIndicator = indicator,
                        addressLine1 = Some("line 1"))
                    )))))

          val result = await(liveSaIncomeService.fetchSaIncomeSources(liveMatchId, taxYearInterval))

          result shouldBe Seq(
            v1.SaIncomeSources(
              TaxYear("2018-19"),
              Seq(
                SaIncomeSource(
                  anSAReturn.utr,
                  Some("thing"),
                  Some(SourceAddress(line1 = Some("line 1"), addressType = addressType)),
                  None
                )
              )))
      }
    }

    "return an empty list when no tax returns exist for the requested period" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(successful(MatchedCitizen(liveMatchId, liveNino)))
      given(mockCache.fetchAndGetEntry[Seq[DesSAIncome]](eqTo(saCacheId.id))(any()))
        .willReturn(Future.successful(None))
      given(desConnector.fetchSelfAssessmentIncome(liveNino, taxYearInterval)).willReturn(successful(Nil))

      val result = await(liveSaIncomeService.fetchSaIncomeSources(liveMatchId, taxYearInterval))
      result shouldBe empty
    }

    "throw a MatchNotFoundException when the matchId is invalid" in new Setup {
      given(matchingConnector.resolve(liveMatchId)).willReturn(Future.failed(new MatchNotFoundException()))

      intercept[MatchNotFoundException] {
        await(liveSaIncomeService.fetchSaIncomeSources(liveMatchId, taxYearInterval))
      }
    }
  }

  trait Setup extends TestData {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val matchingConnector = mock[IndividualsMatchingApiConnector]
    val desConnector = mock[DesConnector]
    val mockCache = mock[ShortLivedCache]
    val mockConfig = mock[CacheRepositoryConfiguration]
    val cacheService = new CacheService(mockCache, mockConfig)
    val sandboxSaIncomeService = new SandboxSaIncomeService()
    val liveSaIncomeService = new LiveSaIncomeService(matchingConnector, desConnector, cacheService, 1)
  }

  trait TestData {
    val utr = SaUtr("2432552644")
    val sandboxMatchId = UUID.fromString("57072660-1df9-4aeb-b4ea-cd2d7f96e430")
    val liveMatchId = UUID.randomUUID()
    val liveNino = Nino("AA100009B")
    val taxYearInterval = TaxYearInterval(TaxYear("2016-17"), TaxYear("2019-20"))
    val saCacheId = SaCacheId(liveNino, taxYearInterval)

    val anSAReturn = DesSAReturn(
      caseStartDate = Some(LocalDate.parse("2017-06-06")),
      receivedDate = Some(LocalDate.parse("2018-10-06")),
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
    )

    val anotherSAReturn = DesSAReturn(
      caseStartDate = Some(LocalDate.parse("2017-06-06")),
      receivedDate = Some(LocalDate.parse("2019-06-06")),
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
    )

    val desIncomes = Seq(
      DesSAIncome(
        taxYear = "2018",
        returnList = Seq(
          anSAReturn
        )
      ),
      DesSAIncome(
        taxYear = "2019",
        returnList = Seq(
          anotherSAReturn
        )
      )
    )
  }

}
