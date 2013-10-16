package salvo.tree

import salvo.util._

abstract class Area(dir: Path) {
  def init(ignoreExisting: Boolean = false) {
    val mkdir = mkdirOrElse(ignoreExisting) _
    mkdir(dir)
  }
  def validate() {
    if (!directory(dir)) sys.error("History dir at "+dir+" does not exist")
  }
  def tail() = new Tail(dir)
  def list(version: Option[Version] = None): List[Dir] = Dir.list(dir)(version)
  def apply(version: Version): Option[Dir] = Dir.load(dir)(version)
}
