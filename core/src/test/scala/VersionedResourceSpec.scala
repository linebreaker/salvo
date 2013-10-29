package salvo.core.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import salvo.tree.test._
import salvo.core._

class VersionedResourceSpec extends Specification with TestUtils {
  "a VersionedResource" should {
    "maintain a versioned resource" in new UsingTempDir {
      val tree = new Tree(tempDir)
      tree.init(ignoreExisting = true)
      object HelloResource extends VersionedResource(
        tree,
        create = path => readFrom(path / "hello.txt"): String)
      HelloResource.start()

      def swap() = {
        val version =
          for (dir <- tree.incoming.create(repr = Unpacked)) yield {
            writeTo("Hello, "+dir.version, tree.incoming / (dir -> Unpacked) / "hello.txt")
            tree.incoming.transition(dir.version, state = Dir.Ready)
            tree.append(dir.version)
            tree.activate(dir.version)
            dir.version
          }

        HelloResource.map(identity) must be_==(Right("Hello, "+version.get)).eventually(retries = 20, sleep = 1.seconds)
      }

      swap()
      swap()
      swap()
      swap()
      swap()

      HelloResource.stop()
      success
    }
  }
}
