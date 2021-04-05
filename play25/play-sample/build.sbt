name := "Play 2.5 Monadic Sample"
organization := "io.playmonad"

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % Test

libraryDependencies += "dev.playmonad" %% "play25-monad" % "0.1.2"
