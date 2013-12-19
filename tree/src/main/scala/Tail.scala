package salvo.tree

import salvo.util._
import java.nio.file.{ Path, StandardWatchEventKinds, WatchEvent, WatchKey }
import java.util.concurrent.TimeUnit
import StandardWatchEventKinds._
import scala.collection.JavaConversions._
import salvo.sun.nio.fs.PollingWatchService

class Tail[T](val dir: Path, parse: WatchEvent[_] => Option[T]) {
  def validate() {
    if (!directory(dir)) sys.error("%s doesn't exist or isn't a directory".format(dir))
  }

  private var _watcher = Option.empty[PollingWatchService]
  def watcher = _watcher.getOrElse(sys.error(dir+": start() was not called"))

  def start() {
    if (_watcher.nonEmpty) sys.error(dir+": start() called more than once")
    validate()
    _watcher = Option(new PollingWatchService())
    watcher.register(dir, Array(ENTRY_CREATE, ENTRY_DELETE))
  }

  def stop() {
    watcher.close()
    _watcher = None
  }

  private def withKey[T](key: WatchKey)(f: WatchKey => T): Option[T] =
    if (key == null) None else Some({
      val result = f(key)
      key.reset()
      result
    })

  private def parse(key: WatchKey): List[T] =
    withKey(key) {
      _.pollEvents().foldRight(List.empty[T]) {
        (evt, acc) => parse(evt).map(_ :: acc).getOrElse(acc)
      }
    }.getOrElse(Nil)

  def poll(): List[T] = Option(watcher.poll()).toList.flatMap(parse(_))
  def poll(timeout: Long, unit: TimeUnit): List[T] = Option(watcher.poll(timeout, unit)).toList.flatMap(parse(_))
  def take() = parse(watcher.take())
}

object Tail {
  def coerce[C](pred: WatchEvent[_] => Boolean)(_evt: WatchEvent[_]): Option[WatchEvent[C]] =
    if (pred(_evt)) Some(_evt.asInstanceOf[WatchEvent[C]])
    else None
}

class DirTail(dir: Path) extends Tail(dir, DirTail.parse(dir))

object DirTail {
  def parse(dir: Path)(_evt: WatchEvent[_]): Option[Dir] =
    for {
      new_evt <- Tail.coerce[Path](_.kind() == ENTRY_CREATE)(_evt)
      dir <- Dir(dir / new_evt.context())
    } yield dir
}

class VersionTail(dir: Path) extends Tail(dir, VersionTail.parse(dir))

object VersionTail {
  def parse(dir: Path)(_evt: WatchEvent[_]): Option[Version] =
    for {
      new_evt <- Tail.coerce[Path](_.kind() == ENTRY_CREATE)(_evt)
      dir <- Version(dir / new_evt.context())
    } yield dir
}
