package salvo.cli

import salvo.tree.Version

abstract class Command(val name: String) extends (Config => Unit)

object InitVersion extends Command("init-version") {
  def apply(config: Config) {
    println(Version.now())
  }
}
