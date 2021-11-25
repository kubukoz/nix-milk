import Dependencies._

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    version := "0.1.0",
    ThisBuild / organization := "com.kubukoz",
  )
