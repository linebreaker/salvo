package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetAddress, InetSocketAddress }

class LeechAction(tree: Tree, version: Version, server: InetSocketAddress, duration: Int, addr: InetAddress = oneAddr(ipv4_?)) extends Logging {
  val dist = new Dist(tree)

  class run {
    val leech = new dist.Leech(version, server, addr)
    logger.info("created leech: "+leech)

    def start() {
      leech.start()
    }

    def await() {
        def wait(client: com.turn.ttorrent.client.Client) {
          while (!dist.finished_?(client)) {
            logger.info("client state: "+client.getState+" ("+"%.2f".format(client.getTorrent.getCompletion())+"% complete)")
            Thread.sleep(1000L)
          }
        }
      wait(leech.client)
      leech.client.waitForCompletion()
    }

    def stop() {
      logger.info("stopping leech")
      leech.stop()
      tree.incoming.transition(version, Dir.Ready)
      tree.append(version)
    }
  }

  def apply() = new run
}
