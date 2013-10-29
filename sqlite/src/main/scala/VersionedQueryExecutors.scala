package salvo.sqlite

import salvo.util._
import salvo.tree._
import salvo.core._
import scala.collection.mutable.{ ListBuffer, ConcurrentMap }
import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentHashMap

abstract class Executors(implicit dir: SqliteDir) {
  private val executors: ConcurrentMap[String, Executor] = new ConcurrentHashMap[String, Executor]
  protected class Executor(db: String, readOnly: Boolean) extends SqliteQueryExecutor(db, readOnly) {
    executors += (db -> this)
  }
  def shutdown() {
    executors.values.foreach(_.executor.shutdown())
  }
}

class VersionedQueryExecutors[A <: Executors](tree: Tree, executors: Path => A) extends VersionedResource(
  tree,
  create = executors(_),
  destroy = (_: Resource[A]).fold(_ => (), _.shutdown()))
