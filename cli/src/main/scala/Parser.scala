package salvo.cli

import scopt.OptionParser
import java.io.File
import java.nio.file._

class Parser extends scopt.OptionParser[Config]("salvo") with OptionParserPlus {
  opt[File]("root") abbr ("r") text ("root of salvo directory structure") action (
    (r, c) => c.copy(root = Paths.get(r.toURI).toAbsolutePath()))

  help("help")

  command(InitVersion)
}
