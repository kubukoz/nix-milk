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
      "io.circe" %% "circe-literal" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "ch.qos.logback" % "logback-classic" % "1.2.7",
      compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.11" cross CrossVersion.full),
    ),
  )
