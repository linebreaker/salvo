package salvo.dist

import salvo.util._
import salvo.tree._
import com.turn.ttorrent.client.Client

class Dist(val tree: Tree) extends SeedOps with LeechOps with TorrentsOps {
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

  def finished_?(client: Client) = {
    import Client.ClientState._
    client.getState() match {
      case SHARING | SEEDING | WAITING | VALIDATING => false
      case _                                        => true
    }
  }

  def apply(version: Version) = tree.history(version).map(tree.history / _.path)

  def seed_?(version: Version) = tree.history(version).nonEmpty
}
