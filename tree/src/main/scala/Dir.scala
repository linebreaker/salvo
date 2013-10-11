package salvo.tree

import java.nio.file.{ Path, Paths }
import org.apache.commons.io.FilenameUtils.{ getExtension, getBaseName }

object Dir {
  sealed abstract class State(val ext: String)
  case object Incomplete extends State("incomplete")
  case object Ready extends State("ready")
  object State {
    def apply(path: Path): Option[State] =
      getExtension(path.getFileName().toString /* XXX: ??? */ ) match {
        case ""             => None
        case Incomplete.ext => Some(Incomplete)
        case Ready.ext      => Some(Ready)
        case _              => None
      }
    implicit object ordering extends Ordering[State] {
      def compare(x: State, y: State) =
        (x, y) match {
          case (Ready, Incomplete) => -1
          case (Incomplete, Ready) => 1
          case _                   => 0
        }
    }
  }

  def apply(path: Path): Option[Dir] =
    for {
      version <- Version(getBaseName(path.getFileName().toString))
      state <- State(path)
    } yield Dir(version, state)

  implicit val ordering = Ordering[(Long, Long, State)].on[Dir](dir => (dir.version.major, dir.version.minor, dir.state))

  def init(state: Dir.State = Incomplete) =
    Dir(version = Version.now(), state = state)
}

case class Dir(version: Version, state: Dir.State) {
  lazy val path = Paths.get(version+"."+state.ext)
}
