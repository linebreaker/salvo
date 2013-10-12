package salvo.cli

import scopt.OptionParser

trait OptionParserPlus[T] {
  self: OptionParser[T] =>
  def command(name: String)(op: T => Unit) =
    cmd(name) action {
      (_, c) =>
        op(c)
        c
    }
  def command_!(name: String)(op: => Unit) = command(name)(_ => op)
}
