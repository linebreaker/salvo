package salvo.dist

import salvo.util._
import salvo.tree._
import scala.collection.JavaConversions._
import java.net.{ InetAddress, InetSocketAddress, HttpURLConnection, URL }
import java.io.{ File, FileOutputStream }
import com.turn.ttorrent.common.Torrent
import com.turn.ttorrent.client.{ SharedTorrent, Client }
import org.apache.commons.io.FileUtils.moveToDirectory
import org.apache.commons.io.IOUtils.{ copy => copyStream }

trait LeechOps {
  dist: Dist =>

  class RemoteServer(server: InetSocketAddress) {
    def url(rest: String) = new URL("http://"+server.getHostName()+":"+server.getPort() + rest)
    def current() =
      for {
        versionS <- GET(url("/current-version.do")).string.right
        version <- Right(Version(versionS.trim)).right
      } yield version
    def torrent(version: Version, saveTo: Path) =
      (for (_ <- GET(url("/"+version+".torrent")).save(saveTo).right)
        yield Torrent.load(saveTo)).fold(throw _, identity)
  }

  def remote(server: InetSocketAddress) = new RemoteServer(server)

  class Leech(val version: Version, server: InetSocketAddress, addr: InetAddress = oneAddr(ipv4_?)) {
    def this(server: InetSocketAddress, addr: InetAddress = oneAddr(ipv4_?)) =
      this(remote(server).current().fold(throw _, _.getOrElse(???)), server, addr)
    lazy val remote = new RemoteServer(server)
    lazy val torrent = remote.torrent(version, dir / (version+".torrent"))
    lazy val dest = tree.incoming.create(version, repr = Packed).map(tree.incoming.dir / _.path).getOrElse(???)
    lazy val shared = new SharedTorrent(torrent, dest, false)
    lazy val client = new Client(addr, shared)
    def seed(duration: Int = 3600) = new SecondarySeed(version, duration, addr)
    def start() {
      client.download()
    }
    def stop() {
      client.stop()
    }
  }
}
