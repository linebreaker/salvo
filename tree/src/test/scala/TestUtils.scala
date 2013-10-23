package salvo.tree.test

import salvo.util._
import salvo.tree._
import java.io.{ BufferedInputStream, FileInputStream }
import java.nio.file.{ Files, Paths }
import org.specs2.mutable._
import org.apache.commons.codec.digest.DigestUtils.sha1Hex

trait TestUtils {
  spec =>

  implicit def string2path(path: String) = java.nio.file.Paths.get(path)
  val validVersionString = "20080328044530-12345"
  val validVersion = Version(major = 20080328044530L, minor = 12345L)
  abstract class UsingTempDir(keep: Boolean = false) extends After {
    protected lazy val tempDir = {
      val x = Files.createTempDirectory(Paths.get("/tmp"), spec.getClass.getSimpleName+".")
      println("[%s]: created temp dir: %s".format(spec.getClass.getSimpleName, x))
      x
    }
    def cleanup() {}
    def after =
      if (keep) {
        println("\n\nkeeping test dir: "+tempDir+"\n\n")
      }
      else {
        import org.apache.commons.io.FileUtils.deleteDirectory
        deleteDirectory(tempDir)
        println("[%s]: cleaned up temp dir: %s".format(spec.getClass.getSimpleName, tempDir))
      }
  }

  def digest(path: Path): String = sha1Hex(new BufferedInputStream(new FileInputStream(path)))
}
