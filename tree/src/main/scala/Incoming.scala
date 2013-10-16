package salvo.tree

import salvo.util._

abstract class Incoming(val dir: Path) extends Area(dir) {
  def create(version: Version = Version.now(), state: Dir.State = Dir.Incomplete) = {
    val created = Dir(version, state)
    mkdirOrElse(ignoreExisting = false)(dir / created.path).map(_ => created)
  }

  def transition(version: Version, state: Dir.State) = apply(version).map(Dir.transition(dir)(_, state))
}
