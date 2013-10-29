package salvo.sqlite.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import salvo.tree.test._
import salvo.core._
import salvo.sqlite._

class VersionedQueryExecutorSpec extends Specification with TestUtils {
  import Person._
  "a VersionedQueryExecutor" should {
    "maintain a versioned set of SQLite query executors" in new UsingTempDir {
      override def keep = true

      val tree = new Tree(tempDir)
      tree.init(ignoreExisting = true)

      class MyBirthday(readOnly: Boolean = true)(implicit dir: SqliteDir) extends Executors {
        object Coming extends Executor("coming", readOnly = readOnly)
        object Undecided extends Executor("undecided", readOnly = readOnly)
        object NotComing extends Executor("not_coming", readOnly = readOnly)
      }

      object TestExecutors extends VersionedQueryExecutors(tree, new MyBirthday()(_))

      def populateVersion(op: MyBirthday => Unit): Version =
        tree.incoming.create(repr = Unpacked).map {
          dir =>
            val execs = new MyBirthday(readOnly = false)(tree.incoming / (dir -> Unpacked))
            init(execs.Coming)
            init(execs.Undecided)
            init(execs.NotComing)
            op(execs)
            tree.incoming.transition(dir.version, state = Dir.Ready)
            tree.append(dir.version)
            dir.version
        }.getOrElse(???)

      def verifyNames(qe: MyBirthday => SqliteQueryExecutor, expected: Set[String]) =
        TestExecutors.map(qe).right.map(names) must be_==(Right(expected)).eventually(retries = 20, sleep = 1.seconds)

      TestExecutors.start()

      populateVersion {
        execs =>
          insert(execs.Coming, P1, P2)
          insert(execs.Undecided, P3)
          insert(execs.NotComing, P4, P5)
      }

      verifyNames(_.Coming, names(P1, P2))
      verifyNames(_.Undecided, names(P3))
      verifyNames(_.NotComing, names(P4, P5))

      populateVersion {
        execs =>
          insert(execs.Coming, P1, P4)
          insert(execs.Undecided, P2, P3)
          insert(execs.NotComing, P5)
      }

      verifyNames(_.Coming, names(P1, P4))
      verifyNames(_.Undecided, names(P2, P3))
      verifyNames(_.NotComing, names(P5))

      populateVersion {
        execs =>
          insert(execs.Undecided, P1, P2, P3)
          insert(execs.NotComing, P4, P5)
      }

      verifyNames(_.Coming, Set.empty)
      verifyNames(_.Undecided, names(P1, P2, P3))
      verifyNames(_.NotComing, names(P4, P5))

      TestExecutors.stop()
    }
  }
}

case class Person(id: Int, name: String)

object Person {
  val DDL = "CREATE TABLE people (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)"
  val INSERT = "INSERT INTO people (id, name) VALUES (?, ?)"
  val SELECT = "SELECT id, name FROM people ORDER BY name ASC"

  def fromRow(rs: java.sql.ResultSet): Person =
    Person(rs.getInt("id"), rs.getString("name"))

  def toRow(p: Person, stmt: java.sql.PreparedStatement) {
    stmt.setInt(1, p.id)
    stmt.setString(2, p.name)
  }

  def insert(qe: SqliteQueryExecutor, ps: Person*) =
    qe.connection {
      conn =>
        conn.setAutoCommit(false)
        val stmt = conn.prepareStatement(INSERT)
        for (p <- ps) {
          toRow(p, stmt)
          stmt.execute()
        }
        conn.commit()
    }

  def select(qe: SqliteQueryExecutor): List[Person] =
    qe.executor.select(SELECT)(fromRow).toList

  def names(qe: SqliteQueryExecutor): Set[String] = select(qe).map(_.name).toSet
  def names(ps: Person*): Set[String] = ps.map(_.name).toSet

  val P1 = Person(1, "Ferdinand Alquié")
  val P2 = Person(2, "Alexandre Kojève")
  val P3 = Person(3, "Nicolas d'Autrecourt")
  val P4 = Person(4, "Jules Barthélemy-Saint-Hilaire")
  val P5 = Person(5, "Pierre Laromiguière")

  def init(qe: SqliteQueryExecutor) {
    qe.executor.exec(DDL)
  }
}
