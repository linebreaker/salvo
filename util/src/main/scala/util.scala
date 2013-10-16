package salvo.util

object `package` {
  type File = java.io.File
  type Path = java.nio.file.Path

  implicit def fileToPath(file: File): Path = java.nio.file.Paths.get(file.toURI).toAbsolutePath()
  implicit def pathToFile(path: Path): File = path.toAbsolutePath().toFile

  def exists(path: Path) = path.exists

  def directory(path: Path) = exists(path) && path.isDirectory

  def mkdir(path: Path): Option[Path] =
    if (directory(path)) None
    else Some(path).filter(_ => path.mkdirs())

  def handleExisting(ignoreExisting: Boolean)(path: Path) =
    if (ignoreExisting) Option(path)
    else sys.error(path.toAbsolutePath()+" already exists")

  def mkdirOrElse(ignoreExisting: Boolean)(path: Path) = mkdir(path) orElse handleExisting(ignoreExisting)(path)

  implicit def pimpPath(path: Path) = new {
    pimped =>
    def /(other: Path): Path = path.resolve(other).toAbsolutePath()
    def /(other: File): Path = pimped / (other: Path)
    def /(other: String): Path = path.resolve(other).toAbsolutePath()
  }
}
