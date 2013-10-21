package salvo.core

object `package` {
  type Resource[T] = Either[Throwable, T]
  implicit def resource[T](op: => T): Resource[T] = {
    try { Right(op) }
    catch { case t: Throwable => Left(t) }
  }
}
