package salvo.cli

import salvo.util._
import salvo.tree._
import java.nio.file._
import java.net.{ InetAddress, InetSocketAddress }
import scopt.Read.reads

trait Util {
  def validate(config: Config) = {
    val tree = new Tree(config.root)
    tree.validate()
    tree
  }
  import Dir.State
  implicit val readsDir = reads[Dir](s => Dir(Paths.get(s)).getOrElse(sys.error("could not parse '"+s+"' as a dir")))
  implicit val readsVersion = reads[Version](string2version)
  implicit val readsState = reads[State](s => State(s).getOrElse(sys.error("could not parse '"+s+"' as a state")))
  implicit val readsInetSocketAddress = reads[InetSocketAddress](server)
  implicit val readsInetAddress = reads[InetAddress](InetAddress.getByName(_))
}
