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
    "clean history" in new UsingTempDir {
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      val Some(latest) =
        List.fill(10)(Version.now()).foldLeft(Option.empty[Version]) {
          (_, version) =>
            tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
            tree.incoming.transition(version, state = Dir.Ready)
            tree.append(version)
            Some(version)
        }
      tree.history.clean()
      tree.history.latest() must beSome[Dir].which(_.version == latest)
      tree.history.list().map(_.version) must_== (latest :: Nil)
    }
    "reject versions that are not ready" in new UsingTempDir {
      val version = Version.now()
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
      tree.append(version) must beNone
    }
    "detect latest version" in new UsingTempDir {
      object tree extends Tree(tempDir)
      tree.init()
      tree.validate()
      for (version <- List.fill(10)(Version.now())) {
        tree.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)
        tree.incoming.transition(version, state = Dir.Ready)
        tree.append(version)
        tree.history.latest() must beSome[Dir].which(_.version == version)
      }
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
          writeTo(random(1024 * 1024 * 100), path)
          val digestBefore = digest(path)
          tree.incoming.transition(version, state = Dir.Ready)
          tree.append(version) must beSome[Version]
          tree.history(version).map(tree.history / (_, Unpacked) / "garbage") must beSome[Path].which {
            after => digest(after) must_== digestBefore
          }
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
          writeTo(random(1024 * 1024 * 100), path)
          val digestBefore = digest(path)
          Repr.flip(tree.incoming, version, Unpacked)
          deleteDirectory(tree.incoming(version).map(tree.incoming / (_, Unpacked)).getOrElse(sys.error("something went wrong")))
          tree.incoming.transition(version, state = Dir.Ready)
          tree.append(version) must beSome[Version]
          tree.history(version).map(tree.history / (_, Unpacked) / "garbage") must beSome[Path].which {
            after => digest(after) must_== digestBefore
          }
      }
    }
  }
}
