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
  import Client.ClientState._

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
}
