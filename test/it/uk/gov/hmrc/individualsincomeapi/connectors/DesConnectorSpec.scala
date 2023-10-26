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

package it.uk.gov.hmrc.individualsincomeapi.connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.joda.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, UpstreamErrorResponse}
import uk.gov.hmrc.individualsincomeapi.connector.DesConnector
import uk.gov.hmrc.individualsincomeapi.domain._
import uk.gov.hmrc.individualsincomeapi.domain.des._
import unit.uk.gov.hmrc.individualsincomeapi.util.TestDates
import utils.TestSupport

import scala.concurrent.ExecutionContext.Implicits.global

class DesConnectorSpec
    extends AnyWordSpec with Matchers with BeforeAndAfterEach with MockitoSugar with TestDates with TestSupport {

  val stubPort = sys.env.getOrElse("WIREMOCK", "11122").toInt
  val stubHost = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  val desAuthorizationToken = "DES_TOKEN"
  val desEnvironment = "DES_ENVIRONMENT"
  val clientId = "CLIENT_ID"

  def fakeApplication() =
    new GuiceApplicationBuilder()
      .bindings(bindModules: _*)
      .configure(
        "microservice.services.des.host"                -> "127.0.0.1",
        "microservice.services.des.port"                -> "11122",
        "microservice.services.des.authorization-token" -> desAuthorizationToken,
        "microservice.services.des.environment"         -> desEnvironment
      )
      .build()

  trait Setup {
    implicit val hc = HeaderCarrier()

    val underTest = fakeApplication().injector.instanceOf[DesConnector]
  }

  def externalServices: Seq[String] = Seq.empty

  override def beforeEach(): Unit = {
    wireMockServer.start()
    configureFor(stubHost, stubPort)
  }

  override def afterEach(): Unit =
    wireMockServer.stop()

  val desAddress = DesAddress(
    line1 = Some("Acme House"),
    line2 = Some("23 Acme Street"),
    line3 = Some("Richmond"),
    line4 = Some("Surrey"),
    line5 = Some("UK"),
    postalCode = Some("AI22 9LL")
  )

  val desPayments = Seq(
    DesPayment(
      paymentDate = LocalDate.parse("2016-11-28"),
      totalPayInPeriod = 100,
      weekPayNumber = None,
      monthPayNumber = Some(8)),
    DesPayment(
      paymentDate = LocalDate.parse("2016-12-06"),
      totalPayInPeriod = 50,
      weekPayNumber = Some(49),
      monthPayNumber = None)
  )
  val desEmployment = DesEmployment(
    employerName = Some("Acme Inc"),
    employerAddress = Some(desAddress),
    employerDistrictNumber = Some("123"),
    employerSchemeReference = Some("AI45678"),
    employmentStartDate = Some(LocalDate.parse("2016-01-01")),
    employmentLeavingDate = Some(LocalDate.parse("2016-06-30")),
    employmentPayFrequency = Some(DesEmploymentPayFrequency.M1),
    payments = desPayments
  )

  "fetchEmployments" should {
    val nino = Nino("NA000799C")
    val fromDate = "2016-01-01"
    val toDate = "2017-03-01"
    val interval = toInterval(fromDate, toDate)

    "return the employments" in new Setup {
      stubFor(
        get(urlPathMatching(s"/individuals/nino/$nino/employments/income"))
          .withQueryParam("from", equalTo(fromDate))
          .withQueryParam("to", equalTo(toDate))
          .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $desAuthorizationToken"))
          .withHeader("Environment", equalTo(desEnvironment))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """
             {
               "employments": [
                 {
                   "employerName":"Acme Inc",
                   "employerAddress": {
                     "line1": "Acme House",
                     "line2": "23 Acme Street",
                     "line3": "Richmond",
                     "line4": "Surrey",
                     "line5": "UK",
                     "postalCode": "AI22 9LL"
                   },
                   "employerDistrictNumber": "123",
                   "employerSchemeReference": "AI45678",
                   "employmentStartDate": "2016-01-01",
                   "employmentLeavingDate": "2016-06-30",
                   "employmentPayFrequency": "M1",
                   "payments": [
                     {
                       "paymentDate": "2016-11-28",
                       "totalPayInPeriod": 100,
                       "monthPayNumber": 8
                     },
                     {
                       "paymentDate": "2016-12-06",
                       "totalPayInPeriod": 50,
                       "weekPayNumber": 49
                     }
                   ]
                 }
               ]
             }
          """
              )))

      val result = await(underTest.fetchEmployments(nino, interval))

      result shouldBe Seq(desEmployment)
    }

    "return an empty list when there is no employments" in new Setup {
      stubFor(
        get(urlPathMatching(s"/individuals/nino/$nino/employments/income"))
          .withQueryParam("from", equalTo("2016-01-01"))
          .withQueryParam("to", equalTo("2017-03-01"))
          .willReturn(aResponse().withStatus(404)))

      val result = await(underTest.fetchEmployments(nino, interval))

      result shouldBe Seq.empty
    }

    "fail when DES returns an error" in new Setup {
      stubFor(
        get(urlPathMatching(s"/individuals/nino/$nino/employments/income"))
          .willReturn(aResponse().withStatus(500)))

      intercept[UpstreamErrorResponse] {
        await(underTest.fetchEmployments(nino, interval))
      }
    }

  }

  "fetchSelfAssessmentIncome" should {
    val nino = Nino("NA000799C")
    val startYear = "2016"
    val endYear = "2017"
    val interval = TaxYearInterval(TaxYear("2015-16"), TaxYear("2016-17"))

    "return the self-assessment returns" in new Setup {
      override implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("X-Client-ID" -> clientId)

      stubFor(
        get(urlPathMatching(s"/individuals/nino/$nino/self-assessment/income"))
          .withQueryParam("startYear", equalTo(startYear))
          .withQueryParam("endYear", equalTo(endYear))
          .withHeader(HeaderNames.authorisation, equalTo(s"Bearer $desAuthorizationToken"))
          .withHeader("Environment", equalTo(desEnvironment))
          .withHeader("OriginatorId", equalTo(s"MDTP_CLIENTID=$clientId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """
             [{
               "taxYear": "2016",
               "returnList": [
                 {
                  "utr": "1234567890",
                  "caseStartDate": "2011-05-05",
                  "receivedDate": "2016-06-06",
                  "incomeFromAllEmployments": 166.55
                 }
               ]
             }]
          """
              )))

      val result = await(underTest.fetchSelfAssessmentIncome(nino, interval))

      result shouldBe Seq(
        DesSAIncome(
          "2016",
          Seq(
            DesSAReturn(
              caseStartDate = Some(LocalDate.parse("2011-05-05")),
              receivedDate = Some(LocalDate.parse("2016-06-06")),
              utr = SaUtr("1234567890"),
              income = SAIncome(incomeFromAllEmployments = Some(166.55))
            )
          )
        )
      )
    }

    "return an empty list when there is no self-assessment returns" in new Setup {
      stubFor(
        get(urlPathMatching(s"/individuals/nino/$nino/self-assessment/income"))
          .withQueryParam("startYear", equalTo(startYear))
          .withQueryParam("endYear", equalTo(endYear))
          .willReturn(aResponse().withStatus(404)))

      val result = await(underTest.fetchSelfAssessmentIncome(nino, interval))

      result shouldBe Seq.empty
    }

    "fail when DES returns an error" in new Setup {
      stubFor(
        get(urlPathMatching(s"/individuals/nino/$nino/self-assessment/income"))
          .willReturn(aResponse().withStatus(500)))

      intercept[UpstreamErrorResponse] {
        await(underTest.fetchSelfAssessmentIncome(nino, interval))
      }
    }
  }
}
