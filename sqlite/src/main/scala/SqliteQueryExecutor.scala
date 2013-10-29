package salvo.sqlite

import scala.util.control.Exception.allCatch
import salvo.util._
import salvo.tree._
import salvo.core._
import org.sqlite.{ SQLiteOpenMode, SQLiteConfig, JDBC }
import com.jolbox.bonecp.BoneCPConfig
import java.sql.{ DriverManager, Connection }
import com.novus.jdbc.bonecp.DebonedQueryExecutor
import com.novus.jdbc.sqlite.Sqlite

class SqliteQueryExecutor(db: String, readOnly: Boolean)(implicit dir: SqliteDir) {
  private val path = dir / "%s.sqlite3".format(db)
  private val jdbcUrl = "jdbc:sqlite:%s".format(path.toAbsolutePath)

  private val sqliteConfig = {
    val config = new SQLiteConfig()
    config.setReadOnly(readOnly)
    if (readOnly)
      config.setOpenMode(SQLiteOpenMode.NOMUTEX)
    else {
      config.setOpenMode(SQLiteOpenMode.NOMUTEX)
      config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE)
      config.setSynchronous(SQLiteConfig.SynchronousMode.OFF)
      config.setJournalMode(SQLiteConfig.JournalMode.OFF)
    }
    config
  }

  private val bonecpConfig = {
    val c = new BoneCPConfig(sqliteConfig.toProperties)
    c.setJdbcUrl(jdbcUrl)
    c
  }

  val executor =
    DebonedQueryExecutor[Sqlite](config = bonecpConfig, driver = classOf[JDBC].getName)

  def connection[T](f: Connection => T): Either[Throwable, T] =
    allCatch.either {
      var conn = Option.empty[Connection]
      try {
        conn = Some(DriverManager.getConnection(jdbcUrl, sqliteConfig.toProperties))
        conn.map(f).getOrElse(sys.error("???"))
      }
      finally {
        conn.foreach(_.close())
      }
    }
}
