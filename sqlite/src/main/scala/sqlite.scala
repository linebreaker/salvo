package salvo.sqlite

import salvo.util._

case class SqliteDir(dir: Path)

object `package` {
  implicit def sqlitedir2path(sd: SqliteDir) = new PimpedPath(sd.dir)
  implicit def path2sqlitedir(path: Path): SqliteDir = SqliteDir(path)
}
