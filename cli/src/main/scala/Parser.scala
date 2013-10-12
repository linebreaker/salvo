package salvo.cli

import scopt.OptionParser
import salvo.tree.Version
import java.io.File
import java.nio.file._

class Parser extends scopt.OptionParser[Config]("salvo") with OptionParserPlus[Config] {
  opt[File]("root") abbr ("r") text ("root of salvo directory structure") action (
    (r, c) => c.copy(root = Paths.get(r.toURI).toAbsolutePath()))

  help("help")

  command_!("init-version") {
    println(Version.now())
  }
}
