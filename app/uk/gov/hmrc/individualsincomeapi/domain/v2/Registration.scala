package uk.gov.hmrc.individualsincomeapi.domain.v2

import play.api.libs.json.Json

case class Registration()

object Registration {
  implicit val payrollJsonFormat = Json.format[Registration]
}
