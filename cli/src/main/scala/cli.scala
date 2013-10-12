package salvo.cli

import java.nio.file._

case class Config(root: Path = Paths.get("").toAbsolutePath())

object Main {
  def main(argv: Array[String]) {
    val parser = new Parser
    parser.parse(argv, Config())
  }
}
