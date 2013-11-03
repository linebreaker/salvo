package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetSocketAddress, InetAddress }
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object SeedAction {
  class Seeder(tree: Tree, duration: Int, addr: InetAddress = oneAddr(ipv4_?))(implicit vo: Ordering[Version]) extends Logging {
    private val dist = new Dist(tree)
    private val continue = new AtomicReference(true)
    private def log(msg: => String) = tree.root+": "+msg
    private val tail = new VersionTail(tree.history.dir)
    private def poll() = tail.poll(1, TimeUnit.SECONDS).sorted(vo.reverse).headOption
    private def latest() = tree.history.latest().map(_.version)
    class SeederThread extends Thread(log("SeederThread")) {
      override def run() {
        while (continue.get()) {
          try {
            (poll() orElse latest()) match {
              case Some(version) =>
                val action = new SeedAction(() => new dist.PrimarySeed(version, duration, addr))
                val run = action()
                run.start()
                val server = new dist.Server()
                server.start()
                run.await()
                run.stop()
                server.stop()
              case _ =>
                logger.info(log("waiting for version..."))
            }
          }
          catch {
            case ie: InterruptedException => // do nothing
          }
        }
      }
    }
    private val thread = new SeederThread()
    def start() {
      tail.start()
      thread.start()
    }
    def join() {
      while (thread.isAlive) thread.join()
    }
    def stop() {
      tail.stop()
      continue.set(false)
      thread.interrupt()
    }
  }
}

class SeedAction(seed0: () => Dist#Seed) extends Logging {
  class run {
    val seed = seed0()
    logger.info("created seed: "+seed)

    def start() {
      seed.start()
    }

    def await() {
      while (!Dist.finished_?(seed.client)) {
        logger.info("[ "+seed.trackerURIs.mkString(", ")+" ] seed state: "+seed.client.getState)
        Thread.sleep(1000L)
      }
      seed.client.waitForCompletion()
    }

    def stop() {
      logger.info("stopping seed")
      seed.stop()
    }
  }

  def apply() = new run
}
