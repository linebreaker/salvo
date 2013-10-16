package salvo.tree

import java.nio.file._
import salvo.util._

class Tree(val root: Path) {
  object incoming extends Incoming(root / "incoming")
  object history extends History(root / "history")

  def init(ignoreExisting: Boolean = false) {
    incoming.init(ignoreExisting)
    history.init(ignoreExisting)
  }

  def validate() {
    if (!directory(root)) sys.error("Root at "+root+" does not exist")
    incoming.validate()
    history.validate()
  }
}
