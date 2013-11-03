package salvo.actions

import salvo.util._
import salvo.tree._
import salvo.dist._
import java.net.{ InetAddress, InetSocketAddress }
import com.turn.ttorrent.client.Client

class LeechAction(tree: Tree, version: Option[Version], server: InetSocketAddress, duration: Int, addr: InetAddress = oneAddr(ipv4_?)) extends Logging {
  val dist = new Dist(tree)

  class run {
    val leech = version.map(new dist.Leech(_, server, duration, addr)).getOrElse(new dist.Leech(server, duration, addr))
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
