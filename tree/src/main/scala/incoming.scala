package salvo.tree

import java.nio.file.{ Path, StandardWatchEventKinds, WatchEvent, WatchKey }
import StandardWatchEventKinds._
import scala.collection.JavaConversions._
import org.apache.commons.io.FilenameUtils.getExtension

abstract class IncomingDir(dir: Path) {
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
  }

  val watcher = dir.getFileSystem().newWatchService()
  dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE)

  sealed abstract class ParsedEvent
  case class New(path: Path, state: State) extends ParsedEvent

  def withKey[T](key: WatchKey)(f: WatchKey => T): T = {
    val result = f(key)
    key.reset()
    result
  }

  def parse(key: WatchKey): List[ParsedEvent] =
    if (key == null) Nil else
      withKey(key) {
        _.pollEvents().foldRight(List.empty[ParsedEvent]) {
          (evt, acc) => parse(evt).map(_ :: acc).getOrElse(acc)
        }
      }

  def coerce[C: Manifest](pred: WatchEvent[_] => Boolean)(_evt: WatchEvent[_]): Option[WatchEvent[C]] =
    if (pred(_evt)) Some(_evt.asInstanceOf[WatchEvent[C]])
    else {
      System.err.println(s"coerce[${manifest.runtimeClass.getName}]: unmatched for kind=${_evt.kind()}")
      None
    }

  def parse(_evt: WatchEvent[_]): Option[ParsedEvent] =
    for {
      new_evt <- coerce[Path](_.kind() == ENTRY_CREATE)(_evt)
      path = dir.resolve(new_evt.context())
      state <- State(path)
    } yield New(path, state)

  def poll() = Option(watcher.poll()).map(parse).getOrElse(Nil)
  def take() = parse(watcher.take())
}
