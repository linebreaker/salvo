package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._

class SeedAction(tree: Tree, version: Version, duration: Int) extends Logging {
  val dist = new Dist(tree)

  class run {
    val seed = new dist.PrimarySeed(version, duration)
    logger.info("created seed: "+seed)
    val server = dist.server()

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
