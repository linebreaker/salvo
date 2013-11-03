package salvo.tree

import salvo.util._

abstract class History(val dir: Path) extends Area(dir) {
  def clean() {
    val preserve = latest()
    cleanFiltered(dir => preserve.exists(_ != dir))
  }
}
