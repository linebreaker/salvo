package salvo.core

import salvo.util._
import salvo.tree._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class VersionedResource[A](tree: Tree, create: Path => Resource[A], destroy: Resource[A] => Unit = (x: Resource[A]) => ()) extends Logging {
  private val id = "VersionedResource("+tree.root+")"
  private def log(msg: => String) = id+": "+msg
  case class NoVersion(tree: Tree) extends Exception("no version in "+tree.root)
  def Zero = Left(NoVersion(tree))
  def create_! =
    tree.history.latest().map(dir => dir.version -> (tree.history / (dir, Unpacked))) match {
      case Some((version, path)) => create(path).right.map(version -> _)
      case _                     => Zero
    }
  private val ref = new AtomicReference[Resource[(Version, A)]](create_!)
  def map[B](f: A => B): Resource[B] = ref.get().right.map { case (_, resource) => f(resource) }
  def apply[B](f: A => B = identity _): B = map(f).fold(throw _, identity)
  private val continue = new AtomicReference(true)
  private class Tailer(timeout: Long, unit: TimeUnit) extends Runnable {
    val tail = tree.history.tail()
    tail.start()
    logger.trace(log("initialized "+this+" using "+tail))
    private def activate(version: Version) {
      logger.info(log("activated version: "+version))
      val next = create_!
      logger.trace(log("created resource: "+next))
      val previous = ref.getAndSet(next)
      destroy(previous.right.map(_._2))
      logger.trace(log("destroyed resource: "+previous))
    }
    def run() {
      while (continue.get()) {
        try {
          logger.trace(log("polling for "+timeout+" "+unit+"..."))
          val versions = tail.poll(timeout, unit).sorted(Version.ordering.reverse)
          (versions.headOption, ref.get().right.toOption.map(_._1)) match {
            case (Some(newVersion), Some(existingVersion)) if Version.ordering.gteq(newVersion, existingVersion) => activate(newVersion)
            case (Some(newVersion), None) => activate(newVersion)
            case (newVersion, oldVersion) => logger.trace("taking no action, newVersion="+newVersion+", oldVersion="+oldVersion)
          }
        }
        catch {
          case ie: InterruptedException =>
            logger.trace(log("interrupted!"))
        }
      }
      tail.stop()
    }
  }
  private val tailer = new Thread(new Tailer(1L, TimeUnit.SECONDS), id+"/Tailer")
  def start() {
    tailer.start()
  }
  def stop() {
    logger.trace(log("stopping "+tailer))
    continue.set(false)
    tailer.interrupt()
    destroy(ref.getAndSet(Zero).right.map(_._2))
  }
}

