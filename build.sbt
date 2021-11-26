import Dependencies._

ThisBuild / scalaVersion := "2.13.7"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    version := "0.1.0",
    organization := "com.kubukoz",
    scalacOptions -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % "0.23.6",
      "org.http4s" %% "http4s-blaze-client" % "0.23.6",
      "org.http4s" %% "http4s-circe" % "0.23.6",
      "org.http4s" %% "http4s-dsl" % "0.23.6",
      // "org.apache.commons" % "commons-compress" % "1.21",
      compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.11" cross CrossVersion.full),
    ),
  )
