package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetAddress, InetSocketAddress }
import java.util.concurrent.atomic.AtomicReference
import com.turn.ttorrent.client.Client
import scala.util.control.Exception.allCatch

object LeechAction {
  class Watcher(tree: Tree, server: InetSocketAddress, delay: Long = 1000L, addr: InetAddress = oneAddr(ipv4_?))(implicit vo: Ordering[Version]) extends Logging {
    private val dist = new Dist(tree)
    private val remote = dist.remote(server)
    private val continue = new AtomicReference(true)
    private def log(msg: => String) = tree.root.getFileName+" => "+server+": "+msg
    private def tryRemote() = remote.latest()
    private def tryLocal() = allCatch.either(tree.history.latest().map(_.version))
    type Tried = Either[Throwable, Option[Version]]
    private def foldTry(e: Tried, say: Tried => Unit = _ => ()): Option[Version] = {
      say(e)
      e.fold(_ => None, identity)
    }
    private def latest() = foldTry(tryRemote(), _.fold(bad => logger.warn(log("Tried remote: "+bad)), _ => ()))
    class WatcherThread extends Thread(log("WatcherThread")) {
      setDaemon(true)
      private var current = Option.empty[(Version, Either[SeedAction#run, LeechAction#run])]
      private def beginLeech(version: Version) {
        val action = new LeechAction(tree, version, server, -1, addr)
        current = Some(version -> Right(useAndReturn(action())(_.start())))
      }
      private def beginSeed(version: Version) {
        val action = new SeedAction(() => new dist.SecondarySeed(version, -1, addr))
        current = Some(version -> Left(useAndReturn(action())(_.start())))
      }
      private def end() {
        current match {
          case Some((version, Right(leechRun))) =>
            leechRun.stop(transition = false)
            tree.incoming.drop(version)
          case Some((version, Left(seedRun))) =>
            seedRun.stop()
          case _ =>
        }
      }
      private def newer(next: Version) = current.exists {
        case (version, _) => vo.gt(next, version)
      }
      def client(r: Either[SeedAction#run, LeechAction#run]) = r.fold(_.seed.client, _.leech.client)
      private def inProgress_?(version: Version) = current.exists { case (cv, _) => cv == version }
      private def progress() {
        current match {
          case Some((version, someRun)) =>
            logger.info(log(Dist.status(client = client(someRun), version = Some(version))))
          case _ =>
        }
      }
      private def flip() {
        current match {
          case Some((version, Right(leechRun))) if !Dist.downloading_?(leechRun.leech.client) && Dist.seedingAt100_?(leechRun.leech.client) =>
            logger.info("flipping "+version+" ...")
            leechRun.stop(transition = true)
            beginSeed(version)
          case _ =>
        }
      }
      override def run() {
        while (continue.get()) {
          try {
            progress()
            flip()
            for (latestRemote <- latest()) {
              val latestLocal = foldTry(tryLocal())
              val accept =
                if (inProgress_?(latestRemote)) false
                else latestLocal.map(vo.gt(latestRemote, _)).getOrElse(true)
              logger.info(log("remote="+latestRemote+", local="+latestLocal+": accept="+accept))
              if (accept) {
                end()
                beginLeech(latestRemote)
              }
              else if (current.isEmpty) {
                for (version <- latestLocal) beginSeed(version)
              }
            }
            Thread.sleep(delay)
          }
          catch {
            case ie: InterruptedException => // do nothing
          }
        }
      }
    }
    private val thread = new WatcherThread()
    def start() {
      thread.start()
    }
    def join() {
      while (thread.isAlive) thread.join(delay)
    }
    def stop() {
      continue.set(false)
      thread.interrupt()
    }
  }
}

class LeechAction(tree: Tree, version: Version, server: InetSocketAddress, duration: Int, addr: InetAddress = oneAddr(ipv4_?)) extends Logging {
  val dist = new Dist(tree)

  class run {
    val leech = new dist.Leech(version, server, duration, addr)
    logger.info("created leech: "+leech)

    var started = Option.empty[Long]
    var completed = Option.empty[Long]

    def elapsed() = started match {
      case Some(s) => ((System.currentTimeMillis - s) / 1000L).intValue
      case _       => sys.error("does not compute")
    }

    def remaining() = duration - elapsed()

    def start() {
      leech.start()
      started = Some(System.currentTimeMillis)
    }

    def await() {
        def wait(client: Client)(pred: Client => Boolean) {
          while (pred(client)) {
            logger.info("client state: "+client.getState+" ("+"%.2f".format(client.getTorrent.getCompletion())+"% after "+elapsed()+" s, "+remaining()+" s left)")
            Thread.sleep(1000L)
          }
        }

      wait(leech.client)(Dist.downloading_?)
      wait(leech.client)((remaining() > 0) && !Dist.finished_?(_))

      leech.client.stop()
      leech.client.waitForCompletion()
    }

    def stop(transition: Boolean) {
      logger.info("stopping leech")
      leech.stop()
      if (transition) {
        tree.incoming.transition(leech.version, Dir.Ready)
        tree.append(leech.version)
      }
    }
  }

  def apply() = new run
}
