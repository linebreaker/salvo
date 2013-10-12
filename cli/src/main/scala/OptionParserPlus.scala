package salvo.cli

import scopt.OptionParser

trait OptionParserPlus {
  self: OptionParser[Config] =>
  def command[C <: Command](c: C) = cmd(c.name) action ((_, config) => config.copy(cmd = Some(c)))
}
