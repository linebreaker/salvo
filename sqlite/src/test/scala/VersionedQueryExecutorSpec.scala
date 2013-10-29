package salvo.sqlite.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import salvo.tree.test._
import salvo.core._
import salvo.sqlite._

class VersionedQueryExecutorSpec extends Specification with TestUtils {
  "a VersionedQueryExecutor" should {
    "maintain a versioned set of SQLite query executors" in new UsingTempDir {
      val tree = new Tree(tempDir)
      tree.init(ignoreExisting = true)
      class MyBirthday(implicit dir: SqliteDir) extends Executors {
        object Coming extends Executor("coming", readOnly = true)
        object Undecided extends Executor("undecided", readOnly = true)
        object NotComing extends Executor("not_coming", readOnly = true)
      }
      object TestExecutors extends VersionedQueryExecutors(tree, new MyBirthday()(_))
      success
    }
  }
}

case class Person(id: Int, name: String)
object Person {
  val INSERT = "INSERT INTO people (id, name) VALUES (?, ?)"
  val DDL = "CREATE TABLE people (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)"
  val SELECT = "SELECT id, name FROM people ORDER BY name ASC"
  def fromRow(rs: java.sql.ResultSet): Person =
    Person(rs.getInt("id"), rs.getString("name"))
  def toRow(p: Person, stmt: java.sql.PreparedStatement) {
    stmt.setInt(1, p.id)
    stmt.setString(2, p.name)
  }
  def insert(qe: SqliteQueryExecutor, p: Person) =
    qe.connection {
      conn =>
        conn.setAutoCommit(true)
        val stmt = conn.prepareStatement(INSERT)
        toRow(p, stmt)
        stmt.execute()
    }
  def select(qe: SqliteQueryExecutor): List[Person] =
    qe.executor.select(SELECT)(fromRow).toList
  val People = List(
    Person(1, "Ferdinand Alquié"),
    Person(2, "Alexandre Kojève"),
    Person(3, "Nicolas d'Autrecourt"),
    Person(4, "Jules Barthélemy-Saint-Hilaire"),
    Person(5, "Pierre Laromiguière"))
  def init(qe: SqliteQueryExecutor) {
    qe.executor.exec(DDL)
  }
}
