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
      tree.append(version) must beSome[Version].which(_ == version)
    }
    "reject versions that are not ready" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete)
      tree.append(version) must beNone
    }
    "activate ready versions" in new UsingTempDir {
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      val versions = List.fill(10)(Version.now()).flatMap {
        version =>
          tree.incoming.create(version, state = Dir.Incomplete)
          tree.incoming.transition(version, state = Dir.Ready)
          tree.append(version)
      }
      for (version <- versions) {
        tree.activate(version) must beSome[Version].which(_ == version)
        tree.current() must beSome[Dir].which(_.version == version)
      }
    }
    "unlink current version" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete)
      tree.incoming.transition(version, state = Dir.Ready)
      tree.append(version) must beSome[Version]
      tree.activate(version) must beSome[Version].which(_ == version)
      tree.current() must beSome[Dir].which(_.version == version)
      tree.current.unlink()
      tree.current() must beNone
    }
  }
}
