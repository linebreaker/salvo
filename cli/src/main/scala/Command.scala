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

object Init extends Command("init") with Util with NilLocalConfig {
  def apply(config: Config) {
    val tree = new Tree(config.root)
    tree.init(ignoreExisting = true)
    new Dist(tree).init(ignoreExisting = true)
  }
}

object Clean extends Command("clean") with Util with NilLocalConfig {
  def apply(config: Config) {
    val tree = validate(config)
    tree.history.clean()
    tree.incoming.clean()
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
    val dist = new Dist(tree)
    for (version <- localConfig.version) {
      val server = new dist.Server()
      server.start()

      val action = new SeedAction(() => new dist.PrimarySeed(version, localConfig.duration, localConfig.addr))
      val run = action()
      run.start()
      run.await()
      run.stop()

      server.stop()
    }
  }
}

object LeechVersion extends Command("leech-version") with Util with Logging {
  class LC(var version: Option[Version] = None, var server: Option[InetSocketAddress] = None, var duration: Int = 3600, var addr: InetAddress = oneAddr(ipv4_?)) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    List(
      parser.opt[InetSocketAddress]("server") required () action localConfig.admit(s => localConfig.server = Some(s)),
      parser.opt[Version]("version") optional () action localConfig.admit(v => localConfig.version = Some(v)),
      parser.opt[Int]("duration") action localConfig.admit(d => localConfig.duration = d),
      parser.opt[InetAddress]("addr") action localConfig.admit(a => localConfig.addr = a))
  def apply(config: Config) {
    val tree = validate(config)
    val dist = new Dist(tree)
    for (server <- localConfig.server) {
      val version = localConfig.version.getOrElse(dist.remote(server).latest_!)
      // first leech
      val leech = (new LeechAction(tree, version, server, localConfig.duration, localConfig.addr))()
      leech.start()
      leech.await()
      leech.stop(transition = true)
    }
  }
}

object Watch extends Command("watch") with Util with Logging {
  class LC(var server: Option[InetSocketAddress] = None, var duration: Int = 3600, var delay: Long = 1000L, var addr: InetAddress = oneAddr(ipv4_?)) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    List(
      parser.opt[InetSocketAddress]("server") required () action localConfig.admit(s => localConfig.server = Some(s)),
      parser.opt[Long]("delay") action localConfig.admit(d => localConfig.delay = d),
      parser.opt[InetAddress]("addr") action localConfig.admit(a => localConfig.addr = a))
  def apply(config: Config) {
    val tree = validate(config)
    for (server <- localConfig.server) {
      val watcher = new LeechAction.Watcher(tree, server, localConfig.delay, localConfig.addr)
      watcher.start()
      watcher.join()
    }
  }
}

object Seed extends Command("seed") with Util with Logging {
  class LC(var duration: Int = 3600, var addr: InetAddress = oneAddr(ipv4_?)) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) = (parser.opt[InetAddress]("addr") action localConfig.admit(a => localConfig.addr = a)) :: Nil
  def apply(config: Config) {
    val tree = validate(config)
    val seeder = new SeedAction.Seeder(tree, localConfig.addr)
    seeder.start()
    seeder.join()
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
      val action = new SeedAction(() => new dist.SecondarySeed(version, localConfig.duration, localConfig.addr))
      val run = action()
      run.start()
      run.await()
      run.stop()
    }
  }
}
