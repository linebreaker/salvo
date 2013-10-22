package salvo.core

import salvo.util._
import salvo.tree._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class VersionedResource[A](tree: Tree, create: Tree => Resource[A], destroy: Resource[A] => Unit = (x: Resource[A]) => ()) extends Logging {
  val id = "VersionedResource("+tree.root+")"
  def log(msg: => String) = id+": "+msg
  val ref = new AtomicReference[Resource[A]](create(tree))
  def map[B](f: A => B): Resource[B] = ref.get().right.map(f)
  val continue = new AtomicReference(true)
  class Tailer(timeout: Long, unit: TimeUnit) extends Runnable {
    val tail = tree.history.tail()
    tail.start()
    logger.trace(log("initialized "+this+" using "+tail))
    def activate(version: Version) {
      tree.activate(version)
      logger.trace(log("activated version: "+version))
      val next = create(tree)
      logger.trace(log("created resource: "+next))
      val previous = ref.getAndSet(next)
      destroy(previous)
      logger.trace(log("destroyed resource: "+previous))
    }
    def run() {
      while (continue.get()) {
        try {
          logger.trace(log("polling for "+timeout+" "+unit+"..."))
          val versions = tail.poll(timeout, unit).sorted(Version.ordering.reverse)
          (versions.headOption, tree.current()) match {
            case (Some(newVersion), Some(Dir(existingVersion, _))) if Version.ordering.gt(newVersion, existingVersion) => activate(newVersion)
            case (Some(newVersion), None) => activate(newVersion)
            case _ => {}
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
  val tailer = new Thread(new Tailer(1000L, TimeUnit.SECONDS), id+"/Tailer")
  def start() {
    tailer.start()
  }
  def stop() {
    logger.trace(log("stopping "+tailer))
    continue.set(false)
    tailer.interrupt()
  }
}

