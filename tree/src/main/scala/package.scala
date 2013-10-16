package salvo.tree

object `package` {
  implicit def string2oversion(s: String): Option[Version] = Version(s)
  implicit def string2version(s: String): Version = Version(s).getOrElse(sys.error("unable to parse invalid version: "+s))
}
