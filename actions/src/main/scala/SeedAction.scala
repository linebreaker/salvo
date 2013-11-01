package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetSocketAddress, InetAddress }

class SeedAction(tree: Tree, version: Version, duration: Int, addr: InetAddress = oneAddr(ipv4_?), serverListen: Option[InetSocketAddress] = None) extends Logging {
  val dist = new Dist(tree)

  class run {
    val seed = new dist.PrimarySeed(version, duration, addr)
    logger.info("created seed: "+seed)

    val server = serverListen match {
      case Some(listen) => dist.server(listen)
      case _            => dist.server()
    }

    def start() {
      seed.start()
      server.start()
    }

    def await() {
      while (!dist.finished_?(seed.client)) {
        logger.info("[ "+seed.trackerURIs.mkString(", ")+" ] seed state: "+seed.client.getState)
        Thread.sleep(1000L)
      }
      seed.client.waitForCompletion()
    }

    def stop() {
      server.stop()
      server.join()
      logger.info("stopping seed")
      seed.stop()
    }
  }

  def apply() = new run
}
