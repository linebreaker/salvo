package salvo.dist

import salvo.util._
import salvo.tree._

class Dist(val tree: Tree) extends SeedOps with LeechOps with ServerOps with Logging {
  dist =>

  lazy val dir = tree.root / "torrents"

  def init(ignoreExisting: Boolean = false) {
    tree.init(ignoreExisting)
    val mkdir = mkdirOrElse(ignoreExisting) _
    mkdir(dir)
  }

  def validate() {
    tree.validate()
    if (!directory(dir)) sys.error("Torrents dir at "+dir+" does not exist")
  }
}

object Dist {
  import com.turn.ttorrent.client.Client
  import com.turn.ttorrent.client.peer.{ SharingPeer, Rate }
  import Client.ClientState._
  import scala.collection.JavaConversions._

  def seedingAt100_?(client: Client) =
    client.getState() match {
      case SEEDING if client.getTorrent().getCompletion() == 100.00 => true
      case _ => false
    }

  def finished_?(client: Client) =
    client.getState() match {
      case SHARING | SEEDING | WAITING | VALIDATING => false
      case _                                        => true
    }

  def downloading_?(client: Client) =
    client.getState() match {
      case SHARING | WAITING | VALIDATING => true
      case _                              => false
    }

  def peers(client: Client): List[SharingPeer] =
    client.getPeers().filter(peer => peer != null && peer.isConnected()).toList

  def rate(client: Client, f: SharingPeer => Rate): Float =
    peers(client).map(f(_).get()).sum

  def status(client: Client, version: Option[Version] = None) = {
    version.map(v => "["+v+"]").getOrElse("<no version>")+": "+
      client.getState+
      " ("+"%.2f%%".format(client.getTorrent().getCompletion())+" / "+peers(client).size+") -- "+
      "%.2f%%".format(rate(client, _.getULRate()) / 1024f)+" kb/sec UP -- "+
      "%.2f%%".format(rate(client, _.getDLRate()) / 1024f)+" kb/sec DOWN"
  }
}
