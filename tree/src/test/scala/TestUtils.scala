package salvo.tree.test

import salvo.tree._
import java.nio.file.{ Files, Paths }

trait TestUtils {
  implicit def string2path(path: String) = java.nio.file.Paths.get(path)
  val validVersionString = "20080328044530-12345"
  val validVersion = Version(major = 20080328044530L, minor = 12345L)
  def tempDir() = Files.createTempDirectory(Paths.get("/tmp"), getClass.getSimpleName+".")
}
