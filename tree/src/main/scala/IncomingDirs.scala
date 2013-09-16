package salvo.tree

import java.nio.file.{ Path, StandardWatchEventKinds, WatchEvent, WatchKey }
import StandardWatchEventKinds._
import scala.collection.JavaConversions._
import scalaz._
import Scalaz._

class IncomingDirs(val dir: Path) {
  if (!dir.toFile.exists) dir.toFile.mkdirs()
  if (!dir.toFile.isDirectory) sys.error("%s doesn't exist or isn't a directory".format(dir))
  val watcher = dir.getFileSystem().newWatchService()
  dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE)

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
      path = dir.resolve(new_evt.context())
      dir <- Dir(path)
    } yield dir

  def poll() = Option(watcher.poll()).toList.flatMap(parse(_))
  def take() = parse(watcher.take())
  def close() = watcher.close()
}
