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

    val file = dir / (version+".torrent")
    val torrent: Torrent

    lazy val original = tree.history(version).map(tree.history.dir / _.version.toString).getOrElse(???)
    lazy val shared = new SharedTorrent(torrent, original, true)
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

  class PrimarySeed(version: Version, duration: Int = 3600, addr: InetAddress = oneAddr(ipv4_?), port: Int = new ServerSocket(0).getLocalPort()) extends Seed(version, duration, addr) with Logging {
    protected lazy val trackers = addrs().filter(ipv4_?).map(addr => new Tracker(new InetSocketAddress(addr, port)))
    lazy val trackerURIs = trackers.map(_.getAnnounceUrl().toURI)
    lazy val torrent = tree.history(version).map(tree.history / (_, Packed)).map {
      source =>
        Torrent.create(
          source,
          tree.history.contents(version, Packed).filterNot(directory).map(_.toFile).toList,
          seqAsJavaList(trackerURIs) :: Nil,
          "salvo/"+System.getProperty("user.name"))
    }.getOrElse(sys.error("???"))
    protected def preStart() {
      torrent.save(new FileOutputStream(file))
      for (tracker <- trackers) {
        logger.info("starting tracker: "+tracker.getAnnounceUrl())
        tracker.start()
      }
      useAndReturn(new TrackedTorrent(torrent))(
        tt => trackers.foreach(tracker => tracker.announce(tt)))
    }
    protected def postStop() {
      for (tracker <- trackers) tracker.stop()
    }
  }

  class SecondarySeed(version: Version, duration: Int = 3600, addr: InetAddress = oneAddr(ipv4_?)) extends Seed(version, duration, addr) {
    lazy val torrent = Torrent.load(file)
    lazy val trackerURIs = torrent.getAnnounceList().flatten.toList
    protected def preStart() {}
    protected def postStop() {}
  }
}
