package unit.uk.gov.hmrc.individualsincomeapi.util

import org.scalatest.matchers.should.Matchers
import java.util.UUID
import org.scalacheck.Arbitrary.arbUuid
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.individualsincomeapi.util.UuidValidator
import utils.SpecBase

class UuidValidatorSpec extends SpecBase with Matchers with ScalaCheckPropertyChecks {

  private val invalidUuid = "0-0-0-0-0"

  "Return true on a valid lower-cased UUID" in {
    forAll { uuid: UUID =>
      UuidValidator.validate(uuid.toString) shouldBe true
    }
  }

  "Return true on a valid upper-cased UUID" in {
    forAll { uuid: UUID =>
      UuidValidator.validate(uuid.toString.toUpperCase) shouldBe true
    }
  }

  "Return false on invalid UUID" in {
    UuidValidator.validate(invalidUuid) shouldBe false
  }

}