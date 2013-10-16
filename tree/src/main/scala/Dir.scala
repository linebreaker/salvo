package salvo.tree

import salvo.util._
import java.io.{ File, FileFilter }
import java.nio.file.Paths
import org.apache.commons.io.FilenameUtils.{ getExtension, getBaseName }

object Dir {
  sealed abstract class State(val ext: String)
  case object Incomplete extends State("incomplete")
  case object Ready extends State("ready")
  object State {
    def apply(s: String): Option[State] =
      s match {
        case ""             => None
        case Incomplete.ext => Some(Incomplete)
        case Ready.ext      => Some(Ready)
        case _              => None
      }

    def apply(path: Path): Option[State] =
      getExtension(path.getFileName().toString) match {
        case ""  => Some(Ready)
        case ext => State(ext)
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
      version <- Version(path)
      state <- State(path)
    } yield Dir(version, state)

  implicit val ordering = Ordering[(Long, Long, State)].on[Dir](dir => (dir.version.major, dir.version.minor, dir.state))

  def init(state: Dir.State = Incomplete) =
    Dir(version = Version.now(), state = state)

  def list(root: Path)(version: Option[Version] = None): List[Dir] =
    root.listFiles(new FileFilter {
      def accept(elem: File) =
        Dir(elem).exists(
          d => directory(root / d.path)
            && version.map(v => d.version == v).getOrElse(true))
    }).foldLeft(List.empty[Dir])((dirs, elem) => Dir(elem).toList ::: dirs).sorted

  def load(root: Path)(version: Version): Option[Dir] =
    list(root)(Some(version)) match {
      case dir :: Nil => Some(dir)
      case Nil        => None
      case dirs       => sys.error("more than one dir found with version "+version+": "+dirs.mkString(", "))
    }

  def transition(root: Path)(dir: Dir, state: Dir.State): Dir = {
    val updated =
      (dir.state, state) match {
        case (Incomplete, Ready) => dir.copy(state = Ready)
        case _                   => sys.error("can only transition from "+Incomplete+" to "+Ready)
      }
    mv(root / dir.path, root / updated.path)
    updated
  }
}

case class Dir(version: Version, state: Dir.State) {
  lazy val path = Paths.get(state match {
    case Dir.Incomplete => version+"."+state.ext
    case Dir.Ready      => version.toString
  })
}
