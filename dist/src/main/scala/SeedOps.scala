package salvo.dist

import salvo.util._
import salvo.tree._
import scala.collection.JavaConversions._
import java.io.FileOutputStream
import java.net.{ URI, InetAddress, ServerSocket, InetSocketAddress }
import com.turn.ttorrent.common.Torrent
import com.turn.ttorrent.client.{ SharedTorrent, Client }
import com.turn.ttorrent.tracker.{ Tracker, TrackedTorrent }

trait SeedOps {
  dist: Dist =>

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
    protected lazy val trackers = addrs().filter(ipv4_?).map(addr => new Tracker(new InetSocketAddress(addr, port)))
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
}
