import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import sbtassembly.Plugin._
import AssemblyKeys._

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
    scalaVersion := "2.10.3",
    crossScalaVersions := Seq("2.9.2", "2.10.3"),
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
    }),
    resolvers ++= Seq(
      "max's clubhouse - releases" at "https://raw.github.com/maxaf/repo/master/releases/",
      "max's clubhouse - snapshots" at "https://raw.github.com/maxaf/repo/master/snapshots/",
      "JBoss Thirdparty Releases" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/"
    )
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
  val commons_io = "commons-io" % "commons-io" % "2.4"
  val scopt = "com.github.scopt" %% "scopt" % "3.1.0"
  val ttorrent = "com.turn" % "ttorrent" % "1.3-SNAPSHOT" intransitive()
  val commons_codec = "commons-codec" % "commons-codec" % "1.8"
  val simpleframework = "org.simpleframework" % "simple" % "4.1.21"
  val jargs = "net.sf" % "jargs" % "1.0"
  val slf4j_api = "org.slf4j" % "slf4j-api" % "1.6.4"
  val slf4j_simple = "org.slf4j" % "slf4j-simple" % "1.6.4" % "test"
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.1"

  val UtilDeps = Seq(slf4j_api)
  val TreeDeps = Seq(slf4j_api, commons_io)
  val DistDeps = Seq(ttorrent, commons_codec, jargs, simpleframework, slf4j_simple)
  val CliDeps = Seq(scopt, logback)
}

object SalvoBuild extends Build {
  import BuildSettings._
  import Deps._

  lazy val root = Project(
    id = "salvo", base = file("."),
    settings = buildSettings ++ Seq(publish := {})
  ) aggregate(util, tree, core, dist, cli)

  lazy val util = Project(
    id = "salvo-util", base = file("util"),
    settings = buildSettings ++ Seq(libraryDependencies ++= UtilDeps))

  lazy val tree = Project(
    id = "salvo-tree", base = file("tree"),
    settings = buildSettings ++ Seq(libraryDependencies ++= TreeDeps)
  ) dependsOn(util)

  lazy val core = Project(
    id = "salvo-core", base = file("core"),
    settings = buildSettings) dependsOn(tree)

  lazy val dist = Project(
    id = "salvo-dist", base = file("dist"),
    settings = buildSettings ++ Seq(libraryDependencies ++= DistDeps)
  ) dependsOn(core)

  lazy val cli = Project(
    id = "salvo-cli", base = file("cli"),
    settings = buildSettings ++ Seq(libraryDependencies ++= CliDeps) ++ assemblySettings ++ Seq(
      mainClass in assembly := Some("salvo.cli.Main"),
      test in assembly := {},
      jarName in assembly <<= (name, version) map { (n, v) => s"${n}-${v}.jar" },
      assemblyCacheUnzip in assembly := false,
      assemblyCacheOutput in assembly := false)
  ) dependsOn(dist)
}
