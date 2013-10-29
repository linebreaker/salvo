package salvo.cli

import scopt.OptionDef
import java.net.{ InetAddress, InetSocketAddress }
import java.nio.file._
import salvo.util._
import salvo.tree._
import salvo.dist._
import salvo.actions._

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

object Init extends Command("init") with Util {
  class InitLocalConfig(var exists: Boolean = false) extends LocalConfig
  val localConfig = new InitLocalConfig()
  def init(parser: Parser) =
    (parser.opt[Boolean]("ignore-existing") action localConfig.admit(localConfig.exists = _)) :: Nil

  def apply(config: Config) {
    val tree = new Tree(config.root)
    tree.init(ignoreExisting = localConfig.exists)
    new Dist(tree).init(ignoreExisting = localConfig.exists)
  }
}

object CreateVersion extends Command("create-version") with NilLocalConfig with Util {
  def apply(config: Config) {
    val tree = validate(config)
    for (created <- tree.incoming.create(repr = Unpacked)) println(created.version)
  }
}

object TransitionVersion extends Command("transition-version") with Util {
  class LC(var version: Option[Version] = None, var state: Option[Dir.State] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) = {
    (parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v))) ::
      (parser.opt[Dir.State]("state") required () action localConfig.admit(s => localConfig.state = Some(s))) :: Nil
  }
  def apply(config: Config) {
    val tree = validate(config)
    println("version = "+localConfig.version+"; state = "+localConfig.state)
    for {
      version <- localConfig.version
      state <- localConfig.state
    } tree.incoming.transition(version, state)
  }
}

object AppendVersion extends Command("append-version") with Util {
  class LC(var version: Option[Version] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    (parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v))) :: Nil
  def apply(config: Config) {
    val tree = validate(config)
    for (version <- localConfig.version) tree.append(version)
  }
}

object ActivateVersion extends Command("activate-version") with Util {
  class LC(var version: Option[Version] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    (parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v))) :: Nil
  def apply(config: Config) {
    val tree = validate(config)
    for (version <- localConfig.version) tree.activate(version)
  }
}

object SeedVersion extends Command("seed-version") with Util with Logging {
  class LC(var version: Option[Version] = None, var duration: Int = 3600, var addr: InetAddress = oneAddr(ipv4_?)) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    List(
      parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v)),
      parser.opt[Int]("duration") action localConfig.admit(d => localConfig.duration = d),
      parser.opt[InetAddress]("addr") action localConfig.admit(a => localConfig.addr = a))
  def apply(config: Config) {
    val tree = validate(config)
    for (version <- localConfig.version) {
      val action = new SeedAction(tree, version, localConfig.duration, localConfig.addr)
      val run = action()
      run.start()
      run.await()
      run.stop()
    }
  }
}

object LeechVersion extends Command("leech-version") with Util with Logging {
  class LC(var version: Option[Version] = None, var server: Option[InetSocketAddress] = None, var duration: Int = 3600, var addr: InetAddress = oneAddr(ipv4_?)) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    List(
      parser.opt[InetSocketAddress]("server") required () action localConfig.admit(s => localConfig.server = Some(s)),
      parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v)),
      parser.opt[Int]("duration") action localConfig.admit(d => localConfig.duration = d),
      parser.opt[InetAddress]("addr") action localConfig.admit(a => localConfig.addr = a))
  def apply(config: Config) {
    val tree = validate(config)
    val dist = new Dist(tree)
    for (version <- localConfig.version; server <- localConfig.server) {
      // first leech
      val leech = (new LeechAction(tree, version, server, localConfig.duration, localConfig.addr))()
      leech.start()
      leech.await()
      leech.stop()
      // then serve
      val seed = (new ServeAction(tree, version, localConfig.duration))()
      seed.start()
      seed.await()
      seed.stop()
    }
  }
}

object ServeVersion extends Command("serve-version") with Util with Logging {
  class LC(var version: Option[Version] = None, var duration: Int = 3600, var addr: InetAddress = oneAddr(ipv4_?)) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    List(
      parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v)),
      parser.opt[Int]("duration") action localConfig.admit(d => localConfig.duration = d),
      parser.opt[InetAddress]("addr") action localConfig.admit(a => localConfig.addr = a))
  def apply(config: Config) {
    val tree = validate(config)
    val dist = new Dist(tree)
    for (version <- localConfig.version) {
      val action = new ServeAction(tree, version, localConfig.duration, localConfig.addr)
      val run = action()
      run.start()
      run.await()
      run.stop()
    }
  }
}
