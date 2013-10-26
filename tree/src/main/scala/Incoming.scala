package salvo.tree

import salvo.util._

abstract class Incoming(val dir: Path) extends Area(dir) {
  def create(version: Version = Version.now(), state: Dir.State = Dir.Incomplete, repr: Repr): Option[Dir] = {
    val created = Dir(version, state)
    val path = dir / created.path
    mkdirOrElse(ignoreExisting = false)(repr(path)).map(_ => created)
  }

  def transition(version: Version, state: Dir.State) = {
    for (d <- this(version)) yield {
      Repr.flip(this, version, repr(d))
      Dir.transition(dir)(d, state)
    }
  }
}
