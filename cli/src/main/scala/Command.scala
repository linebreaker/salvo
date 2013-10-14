package salvo.cli

import scopt.OptionDef
import java.nio.file._

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
  import salvo.tree.Version
  def apply(config: Config) {
    println(Version.now())
  }
}

object Init extends Command("init") {
  class InitLocalConfig(var exists: Boolean = false) extends LocalConfig
  val localConfig = new InitLocalConfig()
  def init(parser: Parser) = {
    (parser.opt[Boolean]("exists") abbr ("e") action localConfig.admit(localConfig.exists = _)) :: Nil
  }
  def mkdir(path: Path) {
    val file = path.toFile
    if (file.exists()) {
      if (!localConfig.exists)
        sys.error(path.toAbsolutePath()+" already exists")
    }
    else
      file.mkdirs()
  }
  def apply(config: Config) {
    mkdir(config.root)
    mkdir(config.root.resolve("incoming"))
  }
}
