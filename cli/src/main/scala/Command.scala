package salvo.cli

import scopt.OptionDef
import java.nio.file._
import salvo.util._
import salvo.tree._

abstract class Command(val name: String) extends (Config => Unit) {
  abstract class LocalConfig {
    def admit[T](f: T => Unit): (T, Config) => Config = {
      (x, c) =>
        f(x)
        c
    }
  }
  val localConfig: LocalConfig
  def init(parser: Parser): Seq[OptionDef[_, Config]]
}

trait NilLocalConfig {
  self: Command =>
  object localConfig extends LocalConfig
  def init(parser: Parser) = Nil
}

object InitVersion extends Command("init-version") with NilLocalConfig {
  def apply(config: Config) {
    println(Version.now())
  }
}

object Init extends Command("init") with Util {
  class InitLocalConfig(var exists: Boolean = false) extends LocalConfig
  val localConfig = new InitLocalConfig()
  def init(parser: Parser) =
    (parser.opt[Boolean]("ignore-existing") action localConfig.admit(localConfig.exists = _)) :: Nil

  def apply(config: Config) {
    val mkdir = mkdirOrElse(localConfig.exists) _
    mkdir(config.root)
    mkdir(config.root.resolve("incoming"))
  }
}

object CreateVersion extends Command("create-version") with NilLocalConfig with Util {
  def apply(config: Config) {
    val tree = validate(config)
    for (created <- tree.incoming.create()) println(tree / created)
  }
}

object TransitionVersion extends Command("transition-version") with Util {
  class LC(var dir: Option[Dir] = None, var state: Option[Dir.State] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) = {
    (parser.opt[Dir]("dir") action localConfig.admit(d => localConfig.dir = Some(d))) ::
      (parser.opt[Dir.State]("state") action localConfig.admit(s => localConfig.state = Some(s))) :: Nil
  }
  def apply(config: Config) {
    val tree = validate(config)
    println("dir = "+localConfig.dir+"; state = "+localConfig.state)
    for {
      dir <- localConfig.dir
      state <- localConfig.state
    } tree.incoming.transition(dir.version, state)
  }
}
