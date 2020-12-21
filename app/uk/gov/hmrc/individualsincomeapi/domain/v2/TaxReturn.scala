package uk.gov.hmrc.individualsincomeapi.domain.v2

import play.api.libs.json.Json

case class TaxReturn()

object TaxReturn {
  implicit val payrollJsonFormat = Json.format[TaxReturn]
}
