package salvo.sqlite.test

import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import salvo.tree.test._
import salvo.core._
import salvo.sqlite._

class VersionedQueryExecutorSpec extends Specification with TestUtils {
  "a VersionedResource" should {
    "maintain a versioned resource" in new UsingTempDir {
      val tree = new Tree(tempDir)
      tree.init(ignoreExisting = true)
      class TestExecutors(implicit dir: SqliteDir) extends Executors {
        object Foo extends Executor("foo", readOnly = true)
        object Bar extends Executor("bar", readOnly = true)
        object Baz extends Executor("baz", readOnly = true)
      }
      object TestExecutors extends VersionedQueryExecutors(tree, new TestExecutors()(_))
      success
    }
  }
}
