package salvo.tree.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import Dir._

class DirSpec extends Specification with TestUtils {
  "Dir model" should {
    "parse dir state" in {
      State("ready") must beSome.which(_ == Ready)
      State("incomplete") must beSome.which(_ == Incomplete)
      State("invalid") must beNone
      State("") must beNone
    }
    "parse whole directory" in {
      Dir("invalid") must beNone
      Dir("inavlid.invalid") must beNone
      Dir("1") must beNone
      Dir(validVersionString+".incomplete") must beSome.which(_ == Dir(version = validVersion, state = Incomplete))
      Dir(validVersionString+".ready") must beSome.which(_ == Dir(version = validVersion, state = Ready))
      Dir(validVersionString) must beSome.which(_ == Dir(version = validVersion, state = Ready))
    }
    "sort directories correctly" in {
      val dirs = {
        Dir(version = validVersion, state = Incomplete) ::
          Dir(version = validVersion, state = Ready) ::
          Dir(version = validVersion, state = Ready) ::
          Dir(version = validVersion, state = Incomplete) ::
          Dir(version = validVersion, state = Ready) ::
          Dir(version = validVersion, state = Incomplete) ::
          Nil
      }
      dirs.sorted must_== {
        Dir(version = validVersion, state = Ready) ::
          Dir(version = validVersion, state = Ready) ::
          Dir(version = validVersion, state = Ready) ::
          Dir(version = validVersion, state = Incomplete) ::
          Dir(version = validVersion, state = Incomplete) ::
          Dir(version = validVersion, state = Incomplete) ::
          Nil
      }
    }
    "transition directories" in new UsingTempDir {
      val version = Version.now()
      object incoming extends Incoming(tempDir)
      incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      incoming(version) must beSome[Dir].which(_ == Dir(version, state = Dir.Incomplete))
      incoming.transition(version, state = Dir.Ready)
      incoming(version) must beSome[Dir].which(_ == Dir(version, state = Dir.Ready))
    }
  }
}
