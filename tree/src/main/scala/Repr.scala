package salvo.tree

import salvo.util._
import org.apache.commons.io.FileUtils.copyFile

sealed abstract class Repr(resolve: Path => Path, other: => Repr) {
  def apply(a: Path): Path = resolve(a)
  def matches_?(parent: Path, dir: Dir): Boolean = directory(resolve(parent / dir.path))
  lazy val flip = other
  lazy val pbzip2 = new PBZip2()
  def rename(path: Path): Path
  def process(from: Path, to: Path): Int
}

case object Packed extends Repr(_ / ".packed", other = Unpacked) {
  def rename(path: Path) = path.resolveSibling(path.getFileName().toString().replaceAll("\\.bz2$", ""))
  def process(from: Path, to: Path) = pbzip2.unpack_!(from, to)
}

case object Unpacked extends Repr(_ / ".unpacked", other = Packed) {
  def rename(path: Path) = path.resolveSibling(path.getFileName()+".bz2")
  def process(from: Path, to: Path) = pbzip2.pack_!(from, to)
}

object Repr {
  def flip(area: Area, version: Version, repr: Repr) =
    for {
      from <- area.path(version, repr)
      to <- area.path(version, repr.flip)
    } yield {
      val entries = traverse(from).map(from.relativize)
      for (entry <- entries) {
        if (directory(from / entry)) mkdir(to / entry)
        else repr.process(from / entry, repr.rename(to / entry))
      }
      (from, to)
    }
}
