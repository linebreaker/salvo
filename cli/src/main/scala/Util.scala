package salvo.cli

import salvo.util._
import salvo.tree._
import java.nio.file._

trait Util {
  def validate(config: Config) = {
    if (!directory(config.root)) sys.error("Root at "+config.root+" does not exist")
    val tree = new Tree(config.root)
    tree.validate()
    tree
  }
  import Dir.State
  implicit val readsDir = scopt.Read.reads[Dir](s => Dir(Paths.get(s)).getOrElse(sys.error("could not parse '"+s+"' as a dir")))
  implicit val readsState = scopt.Read.reads[State](s => State(s).getOrElse(sys.error("could not parse '"+s+"' as a state")))
}
