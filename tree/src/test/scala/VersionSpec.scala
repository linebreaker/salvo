package salvo.tree.test

import org.specs2.mutable._
import salvo.tree._

class VersionSpec extends Specification with TestUtils {
  "Version model" should {
    "initialize to expected format" in {
      val calendar = java.util.Calendar.getInstance()
      calendar.set(2008, 2, 28, 4, 45, 30)
      Version.init(calendar) must_== validVersion
    }
    "parse formatted versions" in {
      Version(validVersionString) must beSome.which(_ == validVersion)
      Version("1"+validVersionString) must beNone
      Version("1") must beNone
      Version("") must beNone
    }
  }
}
