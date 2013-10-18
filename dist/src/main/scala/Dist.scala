package salvo.dist

import salvo.util._
import salvo.tree._
import com.turn.ttorrent.common.{ Torrent => TTorrent }
import com.turn.ttorrent.client.{ SharedTorrent, Client }
import com.turn.ttorrent.tracker.{ Tracker, TrackedTorrent }
import scala.collection.JavaConversions._
import java.net.{ URI, InetAddress, InetSocketAddress }
import java.io.FileOutputStream

class Dist(val tree: Tree, trackers: List[URI] = Nil) {
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

  case class Torrent(version: Version, underlying: TTorrent) {
    val file = dir / (version+".torrent")
    def save() = underlying.save(new FileOutputStream(file))
    def shared() = new SharedTorrent(underlying, tree.history.dir, dist.seed_?(version))
    def client(addr: InetAddress = InetAddress.getByName("0.0.0.0")) = new Client(addr, shared())
    def tracker() =
      useAndReturn(new Tracker(new InetSocketAddress(0)))(_.announce(new TrackedTorrent(underlying)))
    def seed(duration: Int) = Seed(tracker(), client(), duration)
  }

  case class Seed(tracker: Tracker, client: Client, duration: Int) {
    import Client.ClientState._
    def finished_? = client.getState() match {
      case SHARING | SEEDING | WAITING => false
      case _                           => true
    }
    def start() {
      tracker.start()
      client.share(duration)
    }
    def stop() {
      client.stop()
      tracker.stop()
    }
  }

  def apply(version: Version) = tree.history(version).map(tree.history / _.path)

  def torrent(version: Version): Option[Torrent] =
    this(version).map {
      source =>
        Torrent(
          version = version,
          underlying = TTorrent.create(
            source,
            tree.history.list(version),
            seqAsJavaList(seqAsJavaList(trackers) :: Nil),
            System.getProperty("user.name")))
    }

  def seed_?(version: Version) = tree.history(version).nonEmpty
}
