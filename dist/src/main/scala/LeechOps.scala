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

  class RemoteServer(server: InetSocketAddress) extends Logging {
    def url(rest: String) = new URL("http://"+server.getHostName()+":"+server.getPort() + rest)
    def latest() =
      for {
        versionS <- GET(url("/latest-version.do")).string.right
        version <- Right(Version(versionS.trim)).right
      } yield version
    def latest_! = latest().fold(throw _, _.getOrElse(???))
    def torrent(version: Version, saveTo: Path): Either[Throwable, Torrent] =
      for (_ <- GET(url("/"+version+".torrent")).save(saveTo).right) yield Torrent.load(saveTo)
    def tryTorrent(version: Version, saveTo: Path, retries: Int = 60, delay: Long = 1000L) = {
        def go() = {
          logger.info("trying to download torrent for "+version+" from "+server+" ...")
          torrent(version, saveTo)
        }
      val saved = (1 to retries).foldLeft(go) {
        case (x @ Left(_), _) =>
          Thread.sleep(delay)
          go()
        case (x, _) => x
      }
      saved.fold(throw _, identity)
    }
  }

  def remote(server: InetSocketAddress) = new RemoteServer(server)

  class Leech(val version: Version, server: InetSocketAddress, duration: Int = 3600, addr: InetAddress = oneAddr(ipv4_?)) {
    lazy val remote = new RemoteServer(server)
    lazy val dest = {
      if (tree.incoming(version).nonEmpty) tree.incoming.drop(version)
      tree.incoming.create(version, repr = Packed).map(tree.incoming.dir / _.path).getOrElse(???)
    }
    lazy val shared = new SharedTorrent(remote.tryTorrent(version, dir / (version+".torrent")), dest, false)
    lazy val client = new Client(addr, shared)
    def seed(seedDuration: Int = 3600) = new SecondarySeed(version, seedDuration, addr)
    def start() {
      client.share(duration)
    }
    def stop() {
      client.stop(true)
    }
  }
}
