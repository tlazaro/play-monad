import sbt.Keys.scalacOptions

inThisBuild(List(
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
))

lazy val scala_2_11Version = "2.11.12"
lazy val scala_2_12Version = "2.12.13"
lazy val scala_2_13Version = "2.13.5"
lazy val scalaVersionsAll  = Seq(scala_2_11Version, scala_2_12Version, scala_2_13Version)

lazy val sharedSettings: Seq[Setting[_]] = Seq[Setting[_]](
  scalaVersion := scala_2_13Version,
  
  javacOptions ++= Seq("-encoding", "UTF-8"),
  fork in Test := false,
  fork := true,
)

lazy val sharedSettings_2_11: Seq[Setting[_]] = sharedSettings ++ Seq[Setting[_]](
  scalaVersion := scala_2_11Version,
  scalacOptions ++= ScalacOptions.allScala2_11,

  libraryDependencies ++= Seq(
    "org.typelevel"              %% "cats-core"     % "2.0.0",
    "com.typesafe.play" %% "play" % "2.5.19",
    "org.scalatest"              %% "scalatest"     % "3.2.7" % "test",
  ),

  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
)

// Play 2.5
lazy val play25_monad = project.in(file("play25/play-monad")).settings(sharedSettings_2_11 ++ Seq[Setting[_]](
  name := "play25-monad"
) :_*)

//lazy val play25_sample = project.in(file("play25/play-sample")).settings(sharedSettings_2_11: _*)

// All projects
lazy val root = project.in(file(".")).settings(sharedSettings: _*).aggregate(play25_monad)
