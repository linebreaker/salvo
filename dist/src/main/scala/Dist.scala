package salvo.dist

import salvo.util._
import salvo.tree._
import com.turn.ttorrent.common.Torrent
import com.turn.ttorrent.client.{ SharedTorrent, Client }
import com.turn.ttorrent.tracker.{ Tracker, TrackedTorrent }
import scala.collection.JavaConversions._
import java.net.{ URI, InetAddress, InetSocketAddress, ServerSocket }
import java.io.FileOutputStream
import org.apache.commons.io.FileUtils.moveToDirectory

class Dist(val tree: Tree) {
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

  object Tracker {
    def apply(port: Int): List[Tracker] = addrs().filter(ipv4_?).map(addr => new Tracker(new InetSocketAddress(addr, port)))
  }

  def finished_?(client: Client) = {
    import Client.ClientState._
    client.getState() match {
      case SHARING | SEEDING | WAITING | VALIDATING => false
      case _                                        => true
    }
  }

  sealed abstract class Seed(protected val version: Version, protected val duration: Int = 3600, protected val addr: InetAddress = oneAddr(ipv4_?)) {
    val trackerURIs: List[URI]

    lazy val torrent = dist(version).map {
      source =>
        Torrent.create(
          source,
          tree.history.list(version),
          seqAsJavaList(trackerURIs) :: Nil,
          "salvo/"+System.getProperty("user.name"))
    }.getOrElse(sys.error("???"))

    lazy val file = useAndReturn(dir / (version+".torrent"))(
      f => torrent.save(new FileOutputStream(f)))

    lazy val shared = new SharedTorrent(torrent, tree.history.dir, true)
    lazy val client = new Client(addr, shared)

    protected def preStart(): Unit
    def start() {
      preStart()
      client.share(duration)
    }

    protected def postStop(): Unit
    def stop() {
      client.stop()
      postStop()
    }
  }

  class PrimarySeed(version: Version, duration: Int = 3600, addr: InetAddress = oneAddr(ipv4_?), port: Int = new ServerSocket(0).getLocalPort()) extends Seed(version, duration, addr) {
    protected lazy val trackers = Tracker(port)
    lazy val trackerURIs = trackers.map(_.getAnnounceUrl().toURI)
    lazy val tracked = useAndReturn(new TrackedTorrent(torrent))(
      tt => trackers.foreach(tracker => tracker.announce(tt)))
    protected def preStart() {
      for (tracker <- trackers) tracker.start()
    }
    protected def postStop() {
      for (tracker <- trackers) tracker.stop()
    }
  }

  class SecondarySeed(version: Version, duration: Int = 3600, addr: InetAddress = oneAddr(ipv4_?), val trackerURIs: List[URI]) extends Seed(version, duration, addr) {
    protected def preStart() {}
    protected def postStop() {}
  }

  class Leech(version: Version, addr: InetAddress = oneAddr(ipv4_?)) {
    val file = dir / (version+".torrent")
    val torrent = Torrent.load(file)
    val dest = tree.incoming / tree.incoming.create(version).map(_.path).getOrElse(sys.error("???"))
    val shared = new SharedTorrent(torrent, dest, false)
    val client = new Client(addr, shared)
    def seed(duration: Int = 3600) = new SecondarySeed(version, duration, addr, torrent.getAnnounceList().flatten.toList)
    def start() {
      client.download()
    }
    def stop() {
      client.stop()
    }
  }

  def apply(version: Version) = tree.history(version).map(tree.history / _.path)

  def seed_?(version: Version) = tree.history(version).nonEmpty
}
