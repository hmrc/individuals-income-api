package uk.gov.hmrc.individualsincomeapi.domain.v2

import play.api.libs.json.Json

case class SelfAssessment(
                           registrations: Seq[Registration],
                           taxReturns: Seq[TaxReturn]
                         )

object SelfAssessment {
  implicit val payrollJsonFormat = Json.format[SelfAssessment]
}