package salvo.tree.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import org.apache.commons.lang3.RandomStringUtils.random
import org.apache.commons.io.FileUtils.deleteDirectory

class TreeSpec extends Specification with TestUtils {
  "Tree" should {
    "append versions to history" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      tree.incoming.transition(version, state = Dir.Ready)
      tree.append(version) must beSome[Version].which(_ == version)
    }
    "reject versions that are not ready" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      tree.append(version) must beNone
    }
    "activate ready versions" in new UsingTempDir {
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      val versions = List.fill(10)(Version.now()).flatMap {
        version =>
          tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
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
      tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      tree.incoming.transition(version, state = Dir.Ready)
      tree.append(version) must beSome[Version]
      tree.activate(version) must beSome[Version].which(_ == version)
      tree.current() must beSome[Dir].which(_.version == version)
      tree.current.unlink()
      tree.current() must beNone
    }
    "preserve packed version contents" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      val pathO = tree.incoming(version).map(tree.incoming / (_, Unpacked) / "garbage")
      pathO must beSome[Path].which {
        path =>
          write(random(1024 * 1024 * 100), path)
          val digestBefore = digest(path)
          tree.incoming.transition(version, state = Dir.Ready)
          tree.append(version) must beSome[Version]
          tree.current() must beNone
          tree.activate(version) must beSome[Version].which(_ == version)
          tree.current() must beSome[Dir].which(_.version == version)
          val digestAfter = digest(tree.current.link / "garbage")
          digestBefore must_== digestAfter
      }
    }
    "preserve repacked version contents" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      val pathO = tree.incoming(version).map(tree.incoming / (_, Unpacked) / "garbage")
      pathO must beSome[Path].which {
        path =>
          write(random(1024 * 1024 * 100), path)
          val digestBefore = digest(path)
          Repr.flip(tree.incoming, version, Unpacked)
          deleteDirectory(tree.incoming(version).map(tree.incoming / (_, Unpacked)).getOrElse(sys.error("something went wrong")))
          tree.incoming.transition(version, state = Dir.Ready)
          tree.append(version) must beSome[Version]
          tree.current() must beNone
          tree.activate(version) must beSome[Version].which(_ == version)
          tree.current() must beSome[Dir].which(_.version == version)
          val digestAfter = digest(tree.current.link / "garbage")
          digestBefore must_== digestAfter
      }
    }
  }
}
