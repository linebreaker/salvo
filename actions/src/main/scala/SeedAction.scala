package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetSocketAddress, InetAddress }

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
