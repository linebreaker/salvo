package salvo.cli

import salvo.util._

case class Config(root: Path = PWD, cmd: Option[Command] = None)

object Main {
  def main(argv: Array[String]) {
    val parser = new Parser
    for {
      config <- parser.parse(argv, Config())
      cmd <- config.cmd
    } cmd(config)
  }
}
