package salvo.tree

import java.nio.file._
import salvo.util._

class Tree(val root: Path) {
  object incoming {
    lazy val dir = root / "incoming"

    def init() = new IncomingDirs(dir)

    def create(version: Version = Version.now(), state: Dir.State = Dir.Incomplete) = {
      val created = Dir(version, state)
      mkdirOrElse(ignoreExisting = false)(dir / created.path).map(_ => created)
    }

    def list(version: Option[Version] = None): List[Dir] = Dir.list(incoming.dir)(version)

    def apply(version: Version): Option[Dir] = Dir.load(incoming.dir)(version)

    def transition(version: Version, state: Dir.State) = apply(version).map(Dir.transition(dir)(_, state))
  }
  def init(ignoreExisting: Boolean = false) {
    val mkdir = mkdirOrElse(ignoreExisting) _
    mkdir(incoming.dir)
  }
  def validate() {
    if (!directory(root)) sys.error("Root at "+root+" does not exist")
    incoming.init().validate()
  }
  def /(dir: Dir): Path = root / dir.path
}
