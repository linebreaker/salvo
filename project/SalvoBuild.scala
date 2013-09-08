import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object Versions {
  val ScalaVersion = "2.10.2"
  val SpecsVersion = "1.6.9"
}

object BuildSettings {
  import Versions._

  def prompt(state: State) =
    "[%s]> ".format(Project.extract(state).currentProject.id)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.bumnetworks",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := ScalaVersion,
    scalacOptions ++= Seq("-deprecation",  "-unchecked", "-feature", "-language:implicitConversions", "-language:reflectiveCalls", "-language:higherKinds"),
    shellPrompt := prompt,
    showTiming := true,
    parallelExecution := true,
    parallelExecution in Test := false,
    testFrameworks += TestFrameworks.Specs,
    libraryDependencies += Deps.specs,
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
  import Versions._

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.0.3"
  val commons_io = "commons-io" % "commons-io" % "2.4"
  val sqlite = "org.xerial" % "sqlite-jdbc" % "3.7.15-M1"
  val specs = "org.scala-tools.testing" %% "specs" % SpecsVersion % "test"

  val TreeDeps = Seq(scalaz, commons_io, specs)
  val CoreDeps = Seq(sqlite, specs)
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
