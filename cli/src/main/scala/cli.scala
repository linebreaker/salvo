package salvo.cli

import java.io.File
import java.nio.file._
import salvo.tree.Version

case class Config(root: Path = Paths.get("").toAbsolutePath())

object Main {
  def main(argv: Array[String]) {
    val parser = new scopt.OptionParser[Config]("salvo") with OptionParserPlus[Config] {
      opt[File]("root") abbr ("r") text ("root of salvo directory structure") action (
        (r, c) => c.copy(root = Paths.get(r.toURI).toAbsolutePath()))

      help("help")

      command_!("init-version") {
        println(Version.now())
      }
    }
    parser.parse(argv, Config())
  }
}
