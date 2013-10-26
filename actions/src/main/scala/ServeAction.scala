package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetAddress, InetSocketAddress }

class ServeAction(tree: Tree, version: Version, duration: Int, addr: InetAddress = oneAddr(ipv4_?)) extends Logging {
  val dist = new Dist(tree)

  class run {
    val seed = new dist.SecondarySeed(version, duration, addr)
    logger.info("created seed: "+seed)

    def start() {
      seed.start()
    }

    def await() {
      while (!dist.finished_?(seed.client)) {
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
