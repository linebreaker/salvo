package salvo.tree

import java.nio.file._
import salvo.util._

class Tree(val root: Path) {
  object incoming extends Incoming(root / "incoming")

  def init(ignoreExisting: Boolean = false) {
    val mkdir = mkdirOrElse(ignoreExisting) _
    mkdir(incoming.dir)
  }

  def validate() {
    if (!directory(root)) sys.error("Root at "+root+" does not exist")
    incoming.tail().validate()
  }

  def /(dir: Dir): Path = root / dir.path
}
