package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetSocketAddress, InetAddress }
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object SeedAction {
  class Seeder(tree: Tree, addr: InetAddress = oneAddr(ipv4_?))(implicit vo: Ordering[Version]) extends Logging {
    private val dist = new Dist(tree)
    private val continue = new AtomicReference(true)
    private def log(msg: => String) = tree.root.getFileName+": "+msg
    private val tail = new VersionTail(tree.history.dir)
    private def poll() = tail.poll(1, TimeUnit.SECONDS).sorted(vo.reverse).headOption
    private def latest() = tree.history.latest().map(_.version)
    class SeederThread extends Thread(log("SeederThread")) {
      private var current = Option.empty[(Version, dist.Server, SeedAction#run)]
      private def begin(version: Version) {
        val action = new SeedAction(() => new dist.PrimarySeed(version, -1, addr))
        current = Some(version, useAndReturn(new dist.Server())(_.start()), useAndReturn(action())(_.start()))
      }
      private def end() {
        for ((_, server, cur) <- current) {
          server.stop()
          cur.stop()
        }
      }
      private def newer(next: Version) = current match {
        case Some((version, _, _)) => vo.gt(next, version)
        case None                  => true
      }
      override def run() {
        latest().foreach(begin)
        while (continue.get()) {
          try {
            poll() match {
              case Some(version) if newer(version) =>
                end()
                begin(version)
              case _ => // do nothing
            }
            logger.info(current match {
              case Some((version, _, cur)) => log(Dist.status(client = cur.seed.client, version = Some(version)))
              case _                       => log("waiting for new version...")
            })
          }
          catch {
            case ie: InterruptedException => // do nothing
          }
        }
        end()
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
