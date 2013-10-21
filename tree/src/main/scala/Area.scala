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
  def list(version: Option[Version] = None): List[Dir] = Dir.list(dir)(version)
  def contents(version: Version): List[File] = apply(version).map(this / _.path).map(_.listFiles.toList).getOrElse(Nil)
  def apply(version: Version): Option[Dir] = Dir.load(dir)(version)
  def /(path: Path) = dir / path
}
