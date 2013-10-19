package salvo.cli

import scopt.OptionDef
import java.nio.file._
import salvo.util._
import salvo.tree._
import salvo.dist._

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
    new Tree(config.root).init(ignoreExisting = localConfig.exists)
  }
}

object CreateVersion extends Command("create-version") with NilLocalConfig with Util {
  def apply(config: Config) {
    val tree = validate(config)
    for (created <- tree.incoming.create()) println(tree.incoming.dir / created.path)
  }
}

object TransitionVersion extends Command("transition-version") with Util {
  class LC(var dir: Option[Dir] = None, var state: Option[Dir.State] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) = {
    (parser.opt[Dir]("dir") required () action localConfig.admit(d => localConfig.dir = Some(d))) ::
      (parser.opt[Dir.State]("state") required () action localConfig.admit(s => localConfig.state = Some(s))) :: Nil
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

object AppendVersion extends Command("append-version") with Util {
  class LC(var dir: Option[Dir] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    (parser.opt[Dir]("dir") required () action localConfig.admit(d => localConfig.dir = Some(d))) :: Nil
  def apply(config: Config) {
    val tree = validate(config)
    for (dir <- localConfig.dir) tree.append(dir.version)
  }
}

object ActivateVersion extends Command("activate-version") with Util {
  class LC(var dir: Option[Dir] = None) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    (parser.opt[Dir]("dir") required () action localConfig.admit(d => localConfig.dir = Some(d))) :: Nil
  def apply(config: Config) {
    val tree = validate(config)
    for (dir <- localConfig.dir) tree.activate(dir.version)
  }
}

object SeedVersion extends Command("seed-version") with Util {
  class LC(var version: Option[Version] = None, var duration: Int = 3600) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) = {
    (parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v))) ::
      (parser.opt[Int]("duration") action localConfig.admit(d => localConfig.duration = d)) ::
      Nil
  }
  def apply(config: Config) {
    val tree = validate(config)
    val dist = new Dist(tree)
    for (version <- localConfig.version) {
      val seed = dist.Seed(version, localConfig.duration)
      println("created seed: "+seed)
      seed.start()
      while (!dist.finished_?(seed.client)) {
        println("[ "+seed.trackers.map(_.getAnnounceUrl()).mkString(", ")+" ] seed state: "+seed.client.getState)
        Thread.sleep(1000L)
      }
      seed.client.waitForCompletion()
      println("stopping seed")
      seed.stop()
    }
  }
}

object LeechVersion extends Command("leech-version") with Util {
  class LC(var version: Option[Version] = None, var duration: Int = 3600) extends LocalConfig
  val localConfig = new LC()
  def init(parser: Parser) =
    (parser.opt[Version]("version") required () action localConfig.admit(v => localConfig.version = Some(v))) :: Nil
  def apply(config: Config) {
    val tree = validate(config)
    val dist = new Dist(tree)
    for (version <- localConfig.version) {
      val leech = dist.Leech(version)
      println("created leech: "+leech)
      leech.start()
      while (!dist.finished_?(leech.client)) {
        println("leech state: "+leech.client.getState)
        Thread.sleep(1000L)
      }
      leech.client.waitForCompletion()
      println("stopping leech")
      leech.stop()
    }
  }
}
