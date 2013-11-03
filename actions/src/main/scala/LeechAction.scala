package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetAddress, InetSocketAddress }
import java.util.concurrent.atomic.AtomicReference
import com.turn.ttorrent.client.Client
import scala.util.control.Exception.allCatch

object LeechAction {
  class Watcher(tree: Tree, server: InetSocketAddress, duration: Int, delay: Long = 1000L, addr: InetAddress = oneAddr(ipv4_?)) extends Logging {
    private val dist = new Dist(tree)
    private val remote = dist.remote(server)
    private val continue = new AtomicReference(true)
    private def log(msg: => String) = tree.root+" => "+server+": "+msg
    class WatcherThread extends Thread(log("WatcherThread")) {
      private def tryRemote() = remote.latest()
      private def tryLocal() = allCatch.either(tree.history.latest().map(_.version))
      type Tried = Either[Throwable, Option[Version]]
      private def foldTry(e: Tried, say: Tried => Unit = _ => ()): Option[Version] = {
        say(e)
        e.fold(_ => None, identity)
      }
      override def run() {
        while (continue.get()) {
          try {
            for (latestRemote <- foldTry(tryRemote(), _.fold(bad => logger.warn(log("Tried remote: "+bad)), _ => ()))) {
              val latestLocal = foldTry(tryLocal())
              val accept = latestLocal.map(Version.ordering.gt(latestRemote, _)).getOrElse(true)
              logger.info(log("remote="+latestRemote+", local="+latestLocal+": accept="+accept))
              if (accept) {
                val action = new LeechAction(tree, latestRemote, server, duration, addr)
                val run = action()
                run.start()
                run.await()
                run.stop()
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

      wait(leech.client)(dist.downloading_?)
      wait(leech.client)((remaining() > 0) && !dist.finished_?(_))

      leech.client.stop()
      leech.client.waitForCompletion()
    }

    def stop() {
      logger.info("stopping leech")
      leech.stop()
      tree.incoming.transition(leech.version, Dir.Ready)
      tree.append(leech.version)
    }
  }

  def apply() = new run
}
