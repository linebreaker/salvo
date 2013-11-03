package salvo.cli

import salvo.util._
import scopt.OptionParser
import java.io.File
import java.nio.file._

class Parser extends scopt.OptionParser[Config]("salvo") {
  def command[C <: Command](c: C) =
    cmd(c.name) action ((_, config) => config.copy(cmd = Some(c))) children (c.init(this): _*)

  opt[Unit]("build-number") abbr ("V") text ("show build number & exit") optional () action {
    (_, _) =>
      println(Build)
      System.exit(0)
      ???
  }

  opt[File]("root") abbr ("r") text ("root of salvo directory structure") required () action (
    (r, c) => c.copy(root = r))

  help("help")

  command(Init)
  command(Clean)
  command(CreateVersion)
  command(TransitionVersion)
  command(AppendVersion)
  command(SeedVersion)
  command(LeechVersion)
  command(Seed)
  command(ServeVersion)
  command(Watch)
}
