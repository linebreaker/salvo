package salvo.tree

import salvo.util._

abstract class Incoming(val dir: Path) {
  def tail() = new Tail(dir)

  def create(version: Version = Version.now(), state: Dir.State = Dir.Incomplete) = {
    val created = Dir(version, state)
    mkdirOrElse(ignoreExisting = false)(dir / created.path).map(_ => created)
  }

  def list(version: Option[Version] = None): List[Dir] = Dir.list(dir)(version)

  def apply(version: Version): Option[Dir] = Dir.load(dir)(version)

  def transition(version: Version, state: Dir.State) = apply(version).map(Dir.transition(dir)(_, state))
}
