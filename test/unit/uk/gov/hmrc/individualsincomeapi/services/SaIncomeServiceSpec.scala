/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.individualsincomeapi.cache.{CacheConfiguration, SaCacheId, SaIncomeCacheService, ShortLivedCache}
import uk.gov.hmrc.individualsincomeapi.connector.{DesConnector, IndividualsMatchingApiConnector}
import uk.gov.hmrc.individualsincomeapi.domain.SandboxIncomeData.sandboxUtr
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.services.{LiveSaIncomeService, SandboxSaIncomeService}
import uk.gov.hmrc.play.test.UnitSpec
import unit.uk.gov.hmrc.individualsincomeapi.util.Dates

import scala.concurrent.ExecutionContext.Implicits.global
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
        incomeFromAllEmployments = None,
        profitFromSelfEmployment = None,
        incomeFromSelfAssessment = Some(35000.55),
        incomeFromTrust = Some(2600.55),
        incomeFromForeign4Sources = Some(500.55),
        profitFromPartnerships = Some(555.55)))),
    DesSAIncome(
      taxYear = "2016",
      returnList = Seq(
        DesSAReturn(
          caseStartDate = LocalDate.parse("2011-06-06"),
          receivedDate = LocalDate.parse("2016-06-06"),
          utr = utr,
          incomeFromAllEmployments = Some(1555.55),
          profitFromSelfEmployment = Some(2500.55),
          incomeFromSelfAssessment = None,
          incomeFromTrust = None,
          incomeFromForeign4Sources = None)))
  )

  trait Setup {
    implicit val hc = HeaderCarrier()
    val matchingConnector = mock[IndividualsMatchingApiConnector]
    val desConnector = mock[DesConnector]
    val shortLivedCache = mock[ShortLivedCache]
    val configuration = mock[CacheConfiguration]
    val saIncomeCacheService = new SaIncomeCacheService(shortLivedCache, configuration)
    val sandboxSaIncomeService = new SandboxSaIncomeService()
    val liveSaIncomeService = new LiveSaIncomeService(matchingConnector, desConnector, saIncomeCacheService)
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
}