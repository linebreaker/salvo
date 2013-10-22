package salvo.cli

import salvo.util._
import scopt.OptionParser
import java.io.File
import java.nio.file._

class Parser extends scopt.OptionParser[Config]("salvo") {
  def command[C <: Command](c: C) =
    cmd(c.name) action ((_, config) => config.copy(cmd = Some(c))) children (c.init(this): _*)

  opt[File]("root") abbr ("r") text ("root of salvo directory structure") required () action (
    (r, c) => c.copy(root = r))

  help("help")

  command(Init)
  command(CreateVersion)
  command(TransitionVersion)
  command(AppendVersion)
  command(ActivateVersion)
  command(SeedVersion)
  command(LeechVersion)
  command(ServeVersion)
}
