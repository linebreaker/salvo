package salvo.cli

import salvo.util._
import salvo.tree._
import java.nio.file._

trait Util {
  def validate(config: Config) = {
    val tree = new Tree(config.root)
    tree.validate()
    tree
  }
  import Dir.State
  implicit val readsDir = scopt.Read.reads[Dir](s => Dir(Paths.get(s)).getOrElse(sys.error("could not parse '"+s+"' as a dir")))
  implicit val readsVersion = scopt.Read.reads[Version](identity)
  implicit val readsState = scopt.Read.reads[State](s => State(s).getOrElse(sys.error("could not parse '"+s+"' as a state")))
}
