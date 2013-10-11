package salvo.cli

import java.io.File
import java.nio.file._
import salvo.tree.Version

case class Config(root: Path = Paths.get("").toAbsolutePath())

object Main {
  def main(argv: Array[String]) {
    val parser = new scopt.OptionParser[Config]("salvo") {
      opt[File]("root") abbr ("r") text ("root of salvo directory structure") action (
        (r, c) => c.copy(root = Paths.get(r.toURI)))
      help("help")
      cmd("version") action ((_, c) => c) children {
        cmd("init") action {
          (_, c) =>
            println(Version.now())
            c
        }
      }
    }
    parser.parse(argv, Config())
  }
}
