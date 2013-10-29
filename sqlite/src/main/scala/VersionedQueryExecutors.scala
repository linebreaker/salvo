package salvo.sqlite

import salvo.util._
import salvo.tree._
import salvo.core._
import scala.collection.mutable.ListBuffer

abstract class Executors(implicit dir: SqliteDir) {
  private val * = ListBuffer.empty[Executor]
  protected class Executor(db: String, readOnly: Boolean) extends SqliteQueryExecutor(db, readOnly) {
    * += this
  }
  def shutdown() {
    * foreach (_.executor.shutdown())
  }
}

class VersionedQueryExecutors[A <: Executors](tree: Tree, executors: Path => A) extends VersionedResource(tree, executors(_), (x: Resource[Executors]) => x.fold(_ => (), _.shutdown()))
