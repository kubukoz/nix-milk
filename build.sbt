import Dependencies._

ThisBuild / scalaVersion := "3.1.0"

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
      "io.circe" %% "circe-generic" % "0.14.1",
      "ch.qos.logback" % "logback-classic" % "1.2.7",
      "is.cir" %% "ciris" % "2.2.1",
      "dev.profunktor" %% "redis4cats-effects" % "1.0.0",
      "dev.profunktor" %% "redis4cats-log4cats" % "1.0.0",
      "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
      compilerPlugin("org.polyvariant" % "better-tostring" % "0.3.11" cross CrossVersion.full),
    ),
    Compile / doc / sources := Nil,
  )
