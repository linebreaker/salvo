package salvo.cli

import scopt.OptionParser
import java.io.File
import java.nio.file._

class Parser extends scopt.OptionParser[Config]("salvo") {
  def command[C <: Command](c: C) =
    cmd(c.name) action ((_, config) => config.copy(cmd = Some(c))) children (c.init(this): _*)

  opt[File]("root") abbr ("r") text ("root of salvo directory structure") action (
    (r, c) => c.copy(root = Paths.get(r.toURI).toAbsolutePath()))

  help("help")

  command(InitVersion)
  command(Init)
}
