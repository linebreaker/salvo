package salvo.tree

import salvo.util._
import org.apache.commons.io.FileUtils.deleteDirectory

abstract class Area(dir: Path) {
  def init(ignoreExisting: Boolean = false) {
    val mkdir = mkdirOrElse(ignoreExisting) _
    mkdir(dir)
  }
  def validate() {
    if (!directory(dir)) sys.error("History dir at "+dir+" does not exist")
  }
  def list(version: Option[Version] = None): List[Dir] = Dir.list(dir)(version)
  def contents(version: Version, repr: Repr): Iterator[Path] =
    this(version).map(d => traverse(this / (d -> repr))).getOrElse(Iterator.empty)
  def apply(version: Version): Option[Dir] = Dir.load(dir)(version)
  def latest()(implicit ord: Ordering[Dir]): Option[Dir] = list().sorted(ord.reverse).headOption
  private def /(next: Dir): Path = dir / next.path
  def /(next: (Dir, Repr)): Path = {
    val bare = this / next._1
    (repr(next._1), next._2) match {
      case (Packed, Unpacked)                       => Unpacked(bare)
      case (Packed, Packed)                         => Packed(bare)
      case (found, requested) if found == requested => found(bare)
      case (found, requested)                       => sys.error(dir+": repr found("+found+"), requested("+requested+") not available")
    }
  }
  def repr(elem: Dir): Repr =
    if (Packed.matches_?(dir, elem)) Packed
    else if (Unpacked.matches_?(dir, elem)) Unpacked
    else sys.error("unable to detect repr for dir="+dir+", elem="+elem)

  def path(version: Version, requested: Repr) =
    for (d <- this(version)) yield requested(this / d)

  def drop(version: Version) {
    for (path <- this(version).map(this / _)) deleteDirectory(path)
  }

  protected def cleanFiltered(filter: Dir => Boolean) = list().filter(filter).map(_.version).foreach(drop)
  def clean(): Unit
}
