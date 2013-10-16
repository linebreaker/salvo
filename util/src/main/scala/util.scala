package salvo.util

import java.io._
import java.nio.file._

object `package` {
  def exists(path: Path) = path.toFile.exists

  def directory(path: Path) = exists(path) && path.toFile.isDirectory

  def mkdir(path: Path): Option[Path] =
    if (directory(path)) None
    else Some(path).filter(_ => path.toFile.mkdirs())

  def handleExisting(ignoreExisting: Boolean)(path: Path) =
    if (ignoreExisting) Option(path)
    else sys.error(path.toAbsolutePath()+" already exists")

  def mkdirOrElse(ignoreExisting: Boolean)(path: Path) = mkdir(path) orElse handleExisting(ignoreExisting)(path)

  implicit def pimpPath(path: Path) = new {
    def /(other: Path) = path.resolve(other).toAbsolutePath()
    def /(other: File) = path.resolve(Paths.get(other.toURI)).toAbsolutePath()
    def /(other: String) = path.resolve(other).toAbsolutePath()
  }
}
