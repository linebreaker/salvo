package salvo.tree

import salvo.util._
import java.nio.file.{ Path, StandardWatchEventKinds, WatchEvent, WatchKey, WatchService }
import StandardWatchEventKinds._
import scala.collection.JavaConversions._
import scalaz._
import Scalaz._

class Tail(val dir: Path) {
  def validate() {
    if (!directory(dir)) sys.error("%s doesn't exist or isn't a directory".format(dir))
  }

  private var _watcher = Option.empty[WatchService]
  def watcher = _watcher.getOrElse(sys.error(dir+": start() was not called"))

  def start() {
    if (_watcher.nonEmpty) sys.error(dir+": start() called more than once")
    validate()
    _watcher = Option(dir.getFileSystem().newWatchService())
    dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE)
  }

  def stop() {
    watcher.close()
    _watcher = None
  }

  private def withKey[T](key: WatchKey)(f: WatchKey => T)(implicit mt: Monoid[T]): T =
    if (key == null) mt.zero else {
      val result = f(key)
      key.reset()
      result
    }

  private def parse(key: WatchKey): List[Dir] =
    withKey(key) {
      _.pollEvents().foldRight(List.empty[Dir]) {
        (evt, acc) => parse(evt).map(_ :: acc).getOrElse(acc)
      }
    }

  private def coerce[C](pred: WatchEvent[_] => Boolean)(_evt: WatchEvent[_]): Option[WatchEvent[C]] =
    if (pred(_evt)) Some(_evt.asInstanceOf[WatchEvent[C]])
    else None

  private def parse(_evt: WatchEvent[_]): Option[Dir] =
    for {
      new_evt <- coerce[Path](_.kind() == ENTRY_CREATE)(_evt)
      dir <- Dir(dir / new_evt.context())
    } yield dir

  def poll() = Option(watcher.poll()).toList.flatMap(parse(_))
  def take() = parse(watcher.take())
}
