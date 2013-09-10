package salvo.tree.test

import org.specs2.mutable._
import salvo.tree._
import Dir._

class DirSpec extends Specification with TestUtils {
  "Dir model" should {
    "parse dir state" in {
      State("whatever.ready") must beSome.which(_ == Ready)
      State("whatever.incomplete") must beSome.which(_ == Incomplete)
      State("whatever.invalid") must beNone
      State("whatever") must beNone
    }
    "parse whole directory" in {
      Dir("invalid") must beNone
      Dir("inavlid.invalid") must beNone
      Dir("1") must beNone
      Dir("1.incomplete") must beSome.which(_ == Dir(version = 1, state = Incomplete))
      Dir("2.ready") must beSome.which(_ == Dir(version = 2, state = Ready))
    }
  }
}
