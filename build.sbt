ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "LLMForge",
    idePackagePrefix := Some("edu.uic.llmforge")
  )
val logbackVersion = "1.5.6"
val typeSafeConfigVersion = "1.4.3"
val jTokkitVersion = "1.1.0"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe" % "config" % typeSafeConfigVersion,
  "com.knuddels" % "jtokkit" % jTokkitVersion,
)