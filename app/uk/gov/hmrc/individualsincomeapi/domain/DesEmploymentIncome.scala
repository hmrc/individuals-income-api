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

package uk.gov.hmrc.individualsincomeapi.domain

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.EmpRef

case class DesEmployments(employments: Seq[DesEmployment])

case class DesAddress(line1: String,
                      postalCode: String,
                      line2: Option[String] = None,
                      line3: Option[String] = None,
                      line4: Option[String] = None,
                      line5: Option[String] = None)

case class DesPayment(paymentDate: LocalDate,
                      totalPayInPeriod: Double,
                      weekPayNumber: Option[Int] = None,
                      monthPayNumber: Option[Int] = None)

case class DesEmployment(payments: Seq[DesPayment],
                         employerName: Option[String] = None,
                         employerAddress: Option[DesAddress] = None,
                         employerDistrictNumber: Option[String] = None,
                         employerSchemeReference: Option[String] = None,
                         employmentStartDate: Option[LocalDate] = None,
                         employmentLeavingDate: Option[LocalDate] = None,
                         employmentPayFrequency: Option[DesEmploymentPayFrequency.Value] = None
                        ) {

  val employerPayeReference = {
    (employerDistrictNumber, employerSchemeReference) match {
      case (Some(districtNumber), Some(schemeReference)) => Some(EmpRef(districtNumber, schemeReference))
      case _ => None
    }
  }
}

object DesEmploymentPayFrequency extends Enumeration {
  val W1, W2, W4, IO, IR, M1, M3, M6, MA = Value
}

object DesEmployments {
  def toPayments(desEmployment: DesEmployment): Seq[Payment] = {
    desEmployment.payments map { payment =>
      Payment(
        taxablePayment = payment.totalPayInPeriod,
        paymentDate = payment.paymentDate,
        employerPayeReference = desEmployment.employerPayeReference,
        monthPayNumber = payment.monthPayNumber,
        weekPayNumber = payment.weekPayNumber)
    }
  }
}