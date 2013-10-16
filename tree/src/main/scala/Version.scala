package salvo.tree

import java.util.Calendar
import scala.util.control.Exception.allCatch

object Version {
  private val seed = System.nanoTime()
  private def minor0() = (System.nanoTime() - seed).abs

  def now() = at()
  def at(calendar: Calendar = Calendar.getInstance()): Version =
    init(
      yyyy = calendar.get(Calendar.YEAR),
      mm = (calendar.get(Calendar.MONTH) + 1),
      dd = calendar.get(Calendar.DAY_OF_MONTH),
      hh = calendar.get(Calendar.HOUR_OF_DAY),
      mi = calendar.get(Calendar.MINUTE),
      ss = calendar.get(Calendar.SECOND))

  def init(yyyy: Long, mm: Long, dd: Long, hh: Long, mi: Long, ss: Long, minor: Long = minor0()): Version =
    Version(
      major =
        yyyy * 100 * 100 * 100 * 100 * 100 +
          mm * 100 * 100 * 100 * 100 +
          dd * 100 * 100 * 100 +
          hh * 100 * 100 +
          mi * 100 +
          ss,
      minor = minor)

  def apply(version: String): Option[Version] = allCatch.opt {
    val regex = """^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})-(\d+)$""".r
    val regex(yyyy, mm, dd, hh, mi, ss, minor) = version
    init(yyyy.toLong, mm.toLong, dd.toLong, hh.toLong, mi.toLong, ss.toLong, minor.toLong)
  }
}

case class Version(major: Long, minor: Long) {
  override lazy val toString = major+"-"+minor
}
