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

  def append(version: Version): Option[Version] =
    incoming(version).flatMap {
      case dir @ Dir(_, Dir.Ready) =>
        history(version) match {
          case Some(_) => sys.error(history+" already contains version "+version)
          case _ =>
            Some(version).filter(_ => mv(incoming.dir / dir.path, history.dir / dir.path))
        }
      case _ => None
    }

  def activate(version: Version): Option[Version] =
    current() match {
      case Some(Dir(activeVersion, _)) if activeVersion == version => Some(activeVersion)
      case _ =>
        history(version).flatMap {
          case dir @ Dir(_, Dir.Ready) =>
            current.delete()
            current.create(history / dir.path)
            Some(version)
        } orElse (sys.error("unable to activate non-existent version "+version))
    }

  object current {
    val link = root / "current"
    def validate(): Boolean = symlink(link) && directory(link)
    def create(path: Path) {
      Files.createSymbolicLink(link, path.toAbsolutePath())
    }
    def delete() {
      if (validate()) Files.delete(link)
    }
    def apply(): Option[Dir] =
      if (validate())
        allCatch.opt(Files.readSymbolicLink(link)).flatMap(Version(_)).flatMap(history(_))
      else None
  }
}
