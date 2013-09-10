package salvo.tree.test

trait TestUtils {
  implicit def string2path(path: String) = java.nio.file.Paths.get(path)
}
