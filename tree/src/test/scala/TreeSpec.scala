package salvo.tree.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._

class TreeSpec extends Specification with TestUtils {
  "Tree" should {
    "append versions to history" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete)
      tree.incoming.transition(version, state = Dir.Ready)
      tree.append(version) must beSome[Version]
    }
    "reject versions that are not ready" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete)
      tree.append(version) must beNone
    }
  }
}
