package salvo.cli

import salvo.util._
import salvo.tree.Tree

trait Util {
  def validate(config: Config) = {
    if (!directory(config.root)) sys.error("Root at "+config.root+" does not exist")
    val tree = new Tree(config.root)
    tree.validate()
    tree
  }
}
