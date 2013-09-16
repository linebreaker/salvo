package salvo.tree.test

import org.specs2.mutable._
import salvo.tree._
import Dir._
import IncomingDirsSpec._
import java.nio.file._
import org.apache.commons.io.FileUtils.deleteDirectory

class IncomingDirsSpec extends Specification with TestUtils {
  "IncomingDirs" should {
    "return no events" in new ctx {
      attempt("no events")(expected = 0)(identity) must beSome.which(_.isEmpty)
    }
    "generate a specific number of events" in {
        def nEvents(n: Int) =
          "generate %d events".format(n) in new NnewDirs(n) with ctx {
            polled must beSome.which(_.size == n)
          }
      nEvents(1)
      nEvents(2)
      nEvents(3)
      nEvents(4)
    }
  }
}

object IncomingDirsSpec {
  trait ctx extends After {
    lazy val root = Files.createTempDirectory(Paths.get("/tmp"), classOf[IncomingDirsSpec].getSimpleName+".")
    lazy val incoming = {
      val in = new IncomingDirs(root.resolve("incoming"))
      println("spawning IncomingDirs in %s".format(in.dir))
      in
    }
    def attempt[T](tag: String)(attempts: Int = 100, delay: Long = 300L, expected: Int)(f: List[Dir] => T): Option[T] = {
      val (satisfied, dirs) =
        (1 to attempts).foldLeft(false -> List.empty[Dir]) {
          case (acc @ (true, _), _) => acc
          case ((satisfied, dirs), attemptNumber) =>
            println("%s: attempt #%d...".format(tag, attemptNumber))
            Thread.sleep(delay)
            val polled = incoming.poll().toList ::: dirs
            val satisfied = polled.size >= expected
            println("%s: satisfied=%s, polled=%d".format(tag, satisfied, polled.size))
            satisfied -> polled
        }
      if (satisfied) Some(f(dirs)) else None
    }
    def after {
      incoming.close()
      deleteDirectory(root.toFile)
      println("cleaned up %s".format(root))
    }
  }
  class NnewDirs(n: Int) {
    self: ctx =>

    def create() {
      val path = incoming.dir.resolve(Dir.init().path)
      val created = path.toFile.mkdirs()
      println("NnewDirs(%d): created %s: %s".format(n, path, created))
    }
    (1 to n).foreach(_ => create())
    lazy val polled = attempt("%d new dirs".format(n))(expected = n)(identity)
  }
}
