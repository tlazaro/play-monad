import sbt.Keys.scalacOptions

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

inThisBuild(
  List(
    organization := "dev.playmonad",
    homepage := Some(url("https://github.com/tlazaro/playmonad")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "tlazaro",
        "Tomas Lazaro",
        "tlazaro18@gmail.com",
        url("https://github.com/tlazaro")
      ),
      Developer(
        "rcano",
        "rcano",
        "ioniviil@gmail.com",
        url("https://github.com/rcano")
      )
    )
  )
)

lazy val scala_2_10Version = "2.10.7"
lazy val scala_2_11Version = "2.11.12"
lazy val scala_2_12Version = "2.12.13"
lazy val scala_2_13Version = "2.13.5"
lazy val scalaVersionsAll  = Seq(scala_2_11Version, scala_2_12Version, scala_2_13Version)

lazy val sharedSettings: Seq[Setting[_]] = Seq[Setting[_]](
  scalaVersion := scala_2_13Version,
  scalacOptions ++= theScalacOptions(scalaVersion.value),
  javacOptions ++= Seq("-encoding", "UTF-8"),
  Test / fork := false,
  fork := true,
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
)

def theScalacOptions(scalaVersion: String): Seq[String] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 13)) => ScalacOptions.allScala2_13
    case Some((2, 12)) => ScalacOptions.allScala2_12
    case Some((2, 11)) => ScalacOptions.allScala2_11
    case Some((2, 10)) => ScalacOptions.allScala2_10
    case _             => throw new IllegalArgumentException(s"Unsupported Scala version $scalaVersion")
  }

def theCatsVersion(scalaVersion: String): String =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 13)) => "2.5.0"
    case Some((2, 12)) => "2.5.0"
    case Some((2, 11)) => "2.0.0"
    case Some((2, 10)) => "1.2.0"
    case _             => throw new IllegalArgumentException(s"Unsupported Scala version $scalaVersion")
  }

// Play 2.3
lazy val play23_monad = project
  .in(file("play23/play-monad"))
  .settings(
    sharedSettings ++ Seq[Setting[_]](
      name := "play23-monad",
      scalaVersion := scala_2_11Version,
      crossScalaVersions := List(scala_2_11Version, scala_2_10Version),
      libraryDependencies ++= Seq(
        "org.typelevel"      %% "cats-core"            % theCatsVersion(scalaVersion.value),
        "com.typesafe.play"  %% "play"                 % "2.3.10",
        "org.scalatest"      %% "scalatest"            % "3.2.7" % "test"
      ),
      addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full),
      // The Typesafe repository
      resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
    ): _*
  )

// Play 2.4
lazy val play24_monad = project
  .in(file("play24/play-monad"))
  .settings(
    sharedSettings ++ Seq[Setting[_]](
      name := "play24-monad",
      scalaVersion := scala_2_11Version,
      crossScalaVersions := List(scala_2_11Version, scala_2_10Version),
      libraryDependencies ++= Seq(
        "org.typelevel"     %% "cats-core" % theCatsVersion(scalaVersion.value),
        "com.typesafe.play" %% "play"      % "2.4.11",
        "org.scalatest"     %% "scalatest" % "3.2.7" % "test"
      ),
      addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
    ): _*
  )

// Play 2.5
lazy val play25_monad = project
  .in(file("play25/play-monad"))
  .settings(
    sharedSettings ++ Seq[Setting[_]](
      name := "play25-monad",
      scalaVersion := scala_2_11Version,
      crossScalaVersions := List(scala_2_11Version),
      libraryDependencies ++= Seq(
        "org.typelevel"     %% "cats-core" % theCatsVersion(scalaVersion.value),
        "com.typesafe.play" %% "play"      % "2.5.19",
        "org.scalatest"     %% "scalatest" % "3.2.7" % "test"
      ),
      addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
    ): _*
  )

// All projects
lazy val root = project
  .in(file("."))
  .settings(
    sharedSettings ++ Seq[Setting[_]](
      publish / skip := true
    ): _*
  )
  .aggregate(play23_monad, play24_monad, play25_monad)
