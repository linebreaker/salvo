package salvo.sqlite

import salvo.util._
import salvo.tree._
import salvo.core._
import scala.collection.mutable.{ ListBuffer, ConcurrentMap }
import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentHashMap
import com.novus.jdbc.QueryExecutor
import com.novus.jdbc.sqlite.Sqlite

abstract class Executors(implicit dir: SqliteDir) {
  private val executors: ConcurrentMap[String, Executor] = new ConcurrentHashMap[String, Executor]
  protected abstract class Executor(val db: String, val readOnly: Boolean)(implicit val dir: SqliteDir) {
    val executor: QueryExecutor[Sqlite]
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
