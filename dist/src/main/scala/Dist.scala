package salvo.dist

import salvo.util._
import salvo.tree._

class Dist(val tree: Tree) {
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
}

