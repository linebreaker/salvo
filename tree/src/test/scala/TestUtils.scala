package salvo.tree.test

import salvo.util._
import salvo.tree._
import java.nio.file.{ Files, Paths }
import org.specs2.mutable._

trait TestUtils {
  spec =>

  implicit def string2path(path: String) = java.nio.file.Paths.get(path)
  val validVersionString = "20080328044530-12345"
  val validVersion = Version(major = 20080328044530L, minor = 12345L)
  trait UsingTempDir extends After {
    protected lazy val tempDir = {
      val x = Files.createTempDirectory(Paths.get("/tmp"), spec.getClass.getSimpleName+".")
      println("[%s]: created temp dir: %s".format(spec.getClass.getSimpleName, x))
      x
    }
    def cleanup() {}
    def after = {
      import org.apache.commons.io.FileUtils.deleteDirectory
      deleteDirectory(tempDir)
      println("[%s]: cleaned up temp dir: %s".format(spec.getClass.getSimpleName, tempDir))
    }
  }
}
