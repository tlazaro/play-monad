import sbt.Keys.scalacOptions

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

inThisBuild(
  List(
    organization := "dev.playmonad",
    homepage := Some(url("https://github.com/tlazaro/play-monad")),
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

lazy val sharedSettings: Seq[Setting[_]] = Seq[Setting[_]](
  scalaVersion := scala_2_13Version,
  scalacOptions ++= theScalacOptions(scalaVersion.value),
  javacOptions ++= Seq("-encoding", "UTF-8"),
  Test / fork := false,
  fork := true,
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.7" % "test",
    "org.typelevel" %% "cats-core" % theCatsVersion(scalaVersion.value)
  ),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
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

def projectTemplate(projectName: String, playVersion: String, scalaVersions: List[String]): Seq[Setting[_]] =
  sharedSettings ++ Seq[Setting[_]](
    name := projectName,
    scalaVersion := scalaVersions.head,
    crossScalaVersions := scalaVersions,
    libraryDependencies += "com.typesafe.play" %% "play" % playVersion
  )

def akkaStreamsPlayMonad(projectName: String, playVersion: String, scalaVersions: List[String]): Seq[Setting[_]] =
  projectTemplate(projectName, playVersion, scalaVersions) ++ Seq[Setting[_]](
    Compile / unmanagedSourceDirectories ++=
      Seq(baseDirectory.value / ".." / "play-akka-streams" / "src" / "main" / "scala")
  )

def iterateesPlayMonad(projectName: String, playVersion: String, scalaVersions: List[String]): Seq[Setting[_]] =
  projectTemplate(projectName, playVersion, scalaVersions) ++ Seq[Setting[_]](
    Compile / unmanagedSourceDirectories ++=
      Seq(baseDirectory.value / ".." / "play-iteratees" / "src" / "main" / "scala")
  )

// Play 2.3
lazy val play23_monad = project
  .in(file("play23-monad"))
  .settings(
    iterateesPlayMonad(
      "play23-monad",
      "2.3.10",
      List(scala_2_11Version, scala_2_10Version)
    ): _*
  )
  .settings(
    resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
  )

// Play 2.4
lazy val play24_monad = project
  .in(file("play24-monad"))
  .settings(
    iterateesPlayMonad(
      "play24-monad",
      "2.4.11",
      List(scala_2_11Version, scala_2_10Version)
    )
  )

// Play 2.5
lazy val play25_monad = project
  .in(file("play25-monad"))
  .settings(
    akkaStreamsPlayMonad(
      "play25-monad",
      "2.5.19",
      List(scala_2_11Version)
    )
  )

// Play 2.6
lazy val play26_monad = project
  .in(file("play26-monad"))
  .settings(
    akkaStreamsPlayMonad(
      "play26-monad",
      "2.6.25",
      List(scala_2_11Version, scala_2_12Version)
    )
  )

// Play 2.7
lazy val play27_monad = project
  .in(file("play27-monad"))
  .settings(
    akkaStreamsPlayMonad(
      "play27-monad",
      "2.7.9",
      List(scala_2_11Version, scala_2_12Version, scala_2_13Version)
    )
  )

// Play 2.8
lazy val play28_monad = project
  .in(file("play28-monad"))
  .settings(
    akkaStreamsPlayMonad(
      "play28-monad",
      "2.8.7",
      List(scala_2_12Version, scala_2_13Version)
    )
  )

// All projects
lazy val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(
    publish / skip := true
  )
  .aggregate(play23_monad, play24_monad, play25_monad, play26_monad, play27_monad, play28_monad)
