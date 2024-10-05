import sbt.Keys.libraryDependencies
import sbtassembly.AssemblyPlugin.autoImport.*

ThisBuild / version := "0.1.0-SNAPSHOT"

Global / excludeLintKeys += idePackagePrefix
Global / excludeLintKeys += test / fork
Global / excludeLintKeys += run / mainClass

ThisBuild / scalaVersion := "3.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "LLMForge",
    idePackagePrefix := Option("edu.uic.llmforge")
  )

// Version Definitions
val logbackVersion = "1.5.6"
val slf4jLoggerVersion = "2.0.12"
val typeSafeConfigVersion = "1.4.3"
val jTokkitVersion = "1.1.0"
val hadoopVersion = "3.3.6"
val breezeVersion = "2.1.0"
val deepLearning4jVersion = "1.0.0-M2.1"
val nd4jApiVersion = "1.0.0-M2.1"
val scalaTestVersion = "3.2.18"
val mockitoVersion = "1.15.0"

// Library Dependencies
libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.slf4j" % "slf4j-api" % slf4jLoggerVersion,
  "com.typesafe" % "config" % typeSafeConfigVersion,
  "com.knuddels" % "jtokkit" % jTokkitVersion,

  // Hadoop Dependencies
  "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion,
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVersion,
  "org.apache.hadoop" % "hadoop-yarn-client" % hadoopVersion,
  "org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % hadoopVersion,

  // Breeze for numerical processing
  "org.scalanlp" %% "breeze" % breezeVersion,

  // Deep Learning Libraries
  "org.deeplearning4j" % "deeplearning4j-core" % deepLearning4jVersion,
  "org.nd4j" % "nd4j-api" % nd4jApiVersion,
  "org.nd4j" % "nd4j-native" % nd4jApiVersion,
  "org.nd4j" % "nd4j-native-platform" % nd4jApiVersion,
  "org.deeplearning4j" % "deeplearning4j-nlp" % deepLearning4jVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
//  "org.mockito" %% "mockito-scala" % mockitoVersion % Test,
  "org.scalamock" %% "scalamock" % "6.0.0" % Test
)

// Main Class Configuration
Compile / mainClass := Option("edu.uic.llmforge.HW1")
run / mainClass := Option("edu.uic.llmforge.HW1")

// Merging Strategies for Assembly
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) =>
    xs match {
      case "MANIFEST.MF" :: Nil => MergeStrategy.discard
      case "services" :: _      => MergeStrategy.concat
      case _                    => MergeStrategy.discard
    }
  case "reference.conf" => MergeStrategy.concat
  case x if x.endsWith(".proto") => MergeStrategy.rename
  case x if x.contains("hadoop") => MergeStrategy.first
  case _ => MergeStrategy.first
}

assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(true)