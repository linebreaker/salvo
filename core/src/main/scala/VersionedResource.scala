package salvo.core

import salvo.util._
import salvo.tree._
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class VersionedResource[A](tree: Tree, init: Tree => Resource[A]) {
  val id = "VersionedResource("+getClass.getSimpleName+" @ "+tree.root+")"
  val ref = new AtomicReference[Resource[A]](init(tree))
  def map[B](f: A => B): Resource[B] = ref.get().right.map(f)
  val continue = new AtomicReference(true)
  class Tailer(timeout: Long, unit: TimeUnit) extends Runnable {
    val tail = tree.history.tail()
    def activate(version: Version) {
      tree.activate(version)
      ref.set(init(tree))
    }
    def run() {
      while (continue.get()) {
        try {
          val versions = tail.poll(timeout, unit).sorted(Version.ordering.reverse)
          (versions.headOption, tree.current()) match {
            case (Some(newVersion), Some(Dir(existingVersion, _))) if Version.ordering.gt(newVersion, existingVersion) => activate(newVersion)
            case (Some(newVersion), None) => activate(newVersion)
            case _ => {}
          }
        }
        catch {
          case ie: InterruptedException => {}
        }
      }
    }
  }
  val tailer = new Thread(new Tailer(1000L, TimeUnit.SECONDS), id+"/Tailer")
  def start() {
    tailer.start()
  }
  def stop() {
    continue.set(false)
    tailer.interrupt()
  }
}

