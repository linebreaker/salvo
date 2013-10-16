package salvo.util

import java.security._
import java.nio.file.{ Paths, Files }

object `package` {
  type File = java.io.File
  type Path = java.nio.file.Path

  val allCatch = scala.util.control.Exception.allCatch

  val PWD = Paths.get("").toAbsolutePath()

  implicit def fileToPath(file: File): Path = Paths.get(file.toURI).toAbsolutePath()
  implicit def pathToFile(path: Path): File = path.toAbsolutePath().toFile

  def exists(path: Path) = path.exists

  def directory(path: Path) = exists(path) && path.isDirectory

  def symlink(path: Path) = Files.isSymbolicLink(path)

  def mkdir(path: Path): Option[Path] =
    if (directory(path)) None
    else Some(path).filter(_ => path.mkdirs())

  def handleExisting(ignoreExisting: Boolean)(path: Path) =
    if (ignoreExisting) Option(path)
    else sys.error(path.toAbsolutePath()+" already exists")

  def mkdirOrElse(ignoreExisting: Boolean)(path: Path) = mkdir(path) orElse handleExisting(ignoreExisting)(path)

  def mv(src: Path, dst: Path) = src.renameTo(dst)

  implicit def pimpPath(path: Path) = new {
    pimped =>
    def /(other: Path): Path = path.resolve(other).toAbsolutePath()
    def /(other: File): Path = pimped / (other: Path)
    def /(other: String): Path = path.resolve(other).toAbsolutePath()
  }

  def priv[T](op: => T): Either[Throwable, T] =
    allCatch.either(
      AccessController.doPrivileged(new PrivilegedAction[T] {
        def run() = op
      }))
}
