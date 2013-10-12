package salvo.cli

import java.nio.file._

case class Config(root: Path = Paths.get("").toAbsolutePath(), cmd: Option[Command] = None)

object Main {
  def main(argv: Array[String]) {
    val parser = new Parser
    for {
      config <- parser.parse(argv, Config())
      cmd <- config.cmd
    } cmd(config)
  }
}
