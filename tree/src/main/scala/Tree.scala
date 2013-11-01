package salvo.tree

import java.nio.file.Files
import salvo.util._

class Tree(val root: Path) {
  object incoming extends Incoming(root / "incoming")

  object history extends History(root / "history") {
    def tail() = new VersionTail(dir)
  }

  def init(ignoreExisting: Boolean = false) {
    incoming.init(ignoreExisting)
    history.init(ignoreExisting)
  }

  def validate() {
    if (!directory(root)) sys.error("Root at "+root+" does not exist")
    incoming.validate()
    history.validate()
  }

  def append(version: Version): Option[Version] =
    incoming(version).flatMap {
      case dir @ Dir(_, Dir.Ready) =>
        history(version) match {
          case Some(_) => sys.error(history+" already contains version "+version)
          case _       => Some(version).filter(_ => mv(incoming.dir / dir.path, history.dir / dir.path))
        }
      case _ => None
    }
}
