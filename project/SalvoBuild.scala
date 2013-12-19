import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import sbtassembly.Plugin._
import AssemblyKeys._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._

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
    version := "0.3.1-SNAPSHOT",
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
    resolvers ++= Seq(
      "max's clubhouse - releases" at "https://raw.github.com/maxaf/repo/master/releases/",
      "max's clubhouse - snapshots" at "https://raw.github.com/maxaf/repo/master/snapshots/",
      "JBoss Thirdparty Releases" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/"
    )
  ) ++ scalariformSettings ++ formatSettings

  lazy val publishSettings = Seq(
    publishTo <<= (version) {
      v =>
      val repo = file(".") / ".." / "repo"
      Some(Resolver.file("repo",
        if (v.trim.endsWith("SNAPSHOT")) repo / "snapshots"
        else repo / "releases"))
    }
  )

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
  val commons_lang = "org.apache.commons" % "commons-lang3" % "3.1"
  val commons_codec = "commons-codec" % "commons-codec" % "1.8"
  val scopt = "com.github.scopt" %% "scopt" % "3.1.0"
  lazy val ttorrent = ProjectRef(file("../ttorrent"), "ttorrent")
  val simpleframework = "org.simpleframework" % "simple" % "4.1.21"
  val jargs = "net.sf" % "jargs" % "1.0"
  val slf4j_api = "org.slf4j" % "slf4j-api" % "1.6.4"
  val slf4j_simple = "org.slf4j" % "slf4j-simple" % "1.6.4"
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.1"
  def jetty(name: String) = "org.eclipse.jetty" % "jetty-%s".format(name) % "9.0.6.v20130930"
  val sqlite_jdbc = "org.xerial" % "sqlite-jdbc" % "3.7.15-M1"
  val novus_jdbc = "com.novus" %% "novus-jdbc-core" % "0.9.5-FINAL"
  val novus_jdbc_bonecp = "com.novus" %% "novus-jdbc-bonecp" % "0.9.5-FINAL"

  val UtilDeps = Seq(slf4j_api, slf4j_simple % "test", commons_io)
  val TreeDeps = Seq(commons_lang % "test", commons_codec % "test")
  val CoreDeps = Seq()
  val DistDeps = Seq(jargs, simpleframework, jetty("server"), jetty("servlet"), commons_codec)
  val SqliteDeps = Seq(sqlite_jdbc, novus_jdbc, novus_jdbc_bonecp)
  val CliDeps = Seq(scopt, logback)
}

object SalvoBuild extends Build {
  import BuildSettings._
  import Deps._

  lazy val root = Project(
    id = "salvo", base = file("."),
    settings = buildSettings ++ Seq(publish := {})
  ) aggregate(util, tree, core, dist, actions, cli, sqlite)

  lazy val util = Project(
    id = "salvo-util", base = file("util"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= UtilDeps))

  lazy val tree = Project(
    id = "salvo-tree", base = file("tree"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= TreeDeps)
  ) dependsOn(util % "test->test;compile->compile")

  lazy val core = Project(
    id = "salvo-core", base = file("core"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= CoreDeps)) dependsOn(tree % "test->test;compile->compile")

  lazy val sqlite = Project(
    id = "salvo-sqlite", base = file("sqlite"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= SqliteDeps)
  ) dependsOn(core % "test->test;compile->compile")

  lazy val dist = Project(
    id = "salvo-dist", base = file("dist"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= DistDeps)
  ) dependsOn(core % "test->test;compile->compile", ttorrent)

  lazy val actions = Project(
    id = "salvo-actions", base = file("actions"),
    settings = buildSettings ++ publishSettings
  ) dependsOn(dist % "test->test;compile->compile")

  lazy val executable = taskKey[File]("create executable bundle from assembly JAR")

  lazy val cliPackagerSettings = packagerSettings ++ Seq(
    maintainer := "Max Afonov <max+salvo@bumnetworks.com>",
    packageSummary := "Salvo",
    packageDescription := """http://upload.wikimedia.org/wikipedia/commons/d/d2/USS_New_Jersey_BB-62_salvo_Jan_1953.jpeg""",
    debianPackageDependencies in Debian ++= Seq("pbzip2 (>= 1.1.6)"),
    linuxPackageMappings in Debian <+= (assembly, executable) map {
      (ass, exe) =>
      packageMapping(
        ass -> ("usr/share/salvo/" + ass.getName),
        exe -> ("usr/bin/" + exe.getName)) withUser "root" withGroup "root" withPerms "0755"
    }
  )

  lazy val cli = Project(
    id = "salvo-cli", base = file("cli"),
    settings = buildSettings ++ Seq(libraryDependencies ++= CliDeps) ++ assemblySettings ++ Seq(
      publish := {},
      mainClass in assembly := Some("salvo.cli.Main"),
      test in assembly := {},
      jarName in assembly <<= (name, version) map { (n, v) => s"${n}-${v}.jar" },
      assemblyCacheUnzip in assembly := false,
      assemblyCacheOutput in assembly := false,
      mergeStrategy in assembly := {
        case "META-INF/MANIFEST.MF" | "META-INF/LICENSE" | "META-INF/BCKEY.DSA" => MergeStrategy.discard
        case PathList("META-INF", x) if x.toLowerCase.endsWith(".sf") => MergeStrategy.discard
        case PathList("META-INF", x) if x.toLowerCase.endsWith(".dsa") => MergeStrategy.discard
        case PathList("META-INF", x) if x.toLowerCase.endsWith(".rsa") => MergeStrategy.discard
        case _ => MergeStrategy.last
      },
      executable <<= (name, version, crossTarget, assembly) map {
        (n, v, ct, ass) =>
        val exe = file("salvo")
        import java.nio.file.{Files, Paths}
        val launcher = s"""#!/usr/bin/env bash
        |SALVO_BUILD=${v}
        |SALVO_JAR=${ass.getName}
        |SALVO_JAR_DIR=$${SALVO_JAR_DIR:-/usr/share/salvo}
        |SALVO_CROSS_TARGET=`dirname $$0`/${Paths.get("").toAbsolutePath().relativize(Paths.get(ct.toURI))}
        |if [ -f $$SALVO_CROSS_TARGET/$$SALVO_JAR ]; then
        |  SALVO_JAR_DIR=$$SALVO_CROSS_TARGET
        |fi
        |JVM_OPTS="-Xmx1g -XX:+UseG1GC"
        |if [ "x$$SALVO_JVM_OPTS" != "x" ]; then
        |  JVM_OPTS=$$SALVO_JVM_OPTS
        |fi
        |exec env java $$JVM_OPTS -Dsalvo.build=$$SALVO_BUILD -jar $$SALVO_JAR_DIR/$$SALVO_JAR $$@
        |""".stripMargin
        IO.writeLines(file = exe, lines = launcher.split("\n").toList, append = false)
        import java.util.HashSet
        import java.nio.file.attribute.PosixFilePermission
        val perms = new HashSet[PosixFilePermission]()
        perms.add(PosixFilePermission.OWNER_READ)
        perms.add(PosixFilePermission.OWNER_WRITE)
        perms.add(PosixFilePermission.OWNER_EXECUTE)
        perms.add(PosixFilePermission.GROUP_READ)
        perms.add(PosixFilePermission.GROUP_EXECUTE)
        perms.add(PosixFilePermission.OTHERS_READ)
        perms.add(PosixFilePermission.OTHERS_EXECUTE)
        Files.setPosixFilePermissions(Paths.get(exe.toURI), perms)
        exe
      }) ++ cliPackagerSettings
  ) dependsOn(actions % "test->test;compile->compile")
}
