package salvo.dist

import salvo.util._
import salvo.tree._
import scala.collection.JavaConversions._
import java.net.InetAddress
import com.turn.ttorrent.common.Torrent
import com.turn.ttorrent.client.{ SharedTorrent, Client }
import org.apache.commons.io.FileUtils.moveToDirectory

trait LeechOps {
  dist: Dist =>

  class Leech(version: Version, addr: InetAddress = oneAddr(ipv4_?)) {
    val file = dir / (version+".torrent")
    val torrent = Torrent.load(file)
    val dest = tree.incoming / tree.incoming.create(version).map(_.path).getOrElse(sys.error("???"))
    val shared = new SharedTorrent(torrent, dest, false)
    val client = new Client(addr, shared)
    def move() {
      val erroneous = dest / version.toString
      val contents: List[File] = erroneous.listFiles.toList
      for (entry <- contents) moveToDirectory(entry, dest, false)
      erroneous.delete()
    }
    def seed(duration: Int = 3600) = new SecondarySeed(version, duration, addr, torrent.getAnnounceList().flatten.toList)
    def start() {
      client.download()
    }
    def stop() {
      client.stop()
    }
  }
}
