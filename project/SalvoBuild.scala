import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

sealed trait ScalaRelease
case object TwoNine extends ScalaRelease
case object TwoTen extends ScalaRelease
object ScalaRelease {
  val scalaVersionRegex = "(\\d+)\\.(\\d+).*".r
  def apply(v: String) = v match {
    case scalaVersionRegex(major, minor) if major.toInt > 2 || (major == "2" && minor.toInt >= 10) => TwoTen
    case _ => TwoNine
  }
}

object BuildSettings {
  def prompt(state: State) =
    "[%s]> ".format(Project.extract(state).currentProject.id)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.bumnetworks",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.10.2",
    crossScalaVersions := Seq("2.9.2", "2.10.2"),
    scalacOptions <++= scalaVersion map {
      sv =>
      ScalaRelease(sv) match {
        case TwoTen =>
          Seq("-deprecation",  "-unchecked", "-feature", "-language:implicitConversions", "-language:reflectiveCalls", "-language:higherKinds")
        case _ => Seq( "-deprecation", "-unchecked" )
      }
    },
    shellPrompt := prompt,
    showTiming := true,
    parallelExecution := true,
    parallelExecution in Test := false,
    libraryDependencies <+= scalaVersion {
      sv => "org.specs2" %% "specs2" % {
        ScalaRelease(sv) match {
          case TwoTen => "2.2"
          case TwoNine => "1.12.4.1"
        }
      } % "test"
    },
    testFrameworks += TestFrameworks.Specs,
    offline := false,
    initialCommands in console in Test := """""",
    publishTo <<= (version, baseDirectory)({
      (v, base) =>
      val repo = base / ".." / "repo"
      Some(Resolver.file("repo",
        if (v.trim.endsWith("SNAPSHOT")) repo / "snapshots"
        else repo / "releases"))
    })
  ) ++ scalariformSettings ++ formatSettings

  lazy val formatSettings = Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  lazy val formattingPreferences = {
    FormattingPreferences().
      setPreference(AlignParameters, true).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(CompactControlReadability, true).
      setPreference(CompactStringConcatenation, true).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(FormatXml, true).
      setPreference(IndentLocalDefs, true).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, true).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(PreserveDanglingCloseParenthesis, false).
      setPreference(RewriteArrowSymbols, false).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesWithinPatternBinders, true)
  }
}

object Deps {
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.3"
  val commons_io = "commons-io" % "commons-io" % "2.4"
  val sqlite = "org.xerial" % "sqlite-jdbc" % "3.7.15-M1"

  val TreeDeps = Seq(scalaz, commons_io)
  val CoreDeps = Seq(sqlite)
}

object SalvoBuild extends Build {
  import BuildSettings._
  import Deps._

  lazy val root = Project(
    id = "salvo", base = file("."),
    settings = buildSettings ++ Seq(publish := {})
  ) aggregate(tree, core)

  lazy val tree = Project(
    id = "salvo-tree", base = file("tree"),
    settings = buildSettings ++ Seq(libraryDependencies ++= TreeDeps)
  )

  lazy val core = Project(
    id = "salvo-core", base = file("core"),
    settings = buildSettings ++ Seq(libraryDependencies ++= CoreDeps)
  ) dependsOn(tree)
}
