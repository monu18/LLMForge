
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "LLMForge",
    idePackagePrefix := Option("edu.uic.llmforge")
  )
val logbackVersion = "1.5.6"
val slf4jLoggerVersion = "2.0.12"
val typeSafeConfigVersion = "1.4.3"
val jTokkitVersion = "1.1.0"
val hadoopClientVersion = "3.4.0"
val hadoopCommonVersion = "3.4.0"
val hadoopHdfsVersion = "3.4.0"
val hadoopMapReduceClientCoreVersion = "3.4.0"
val hadoopYarnClientVersion = "3.4.0"
val hadoopMapReduceJobClientVersion = "3.4.0"
val breezeVersion = "2.1.0"
val deepLearning4jVersion = "1.0.0-M2.1"
val deepLearning4jNLP = "1.0.0-M2.1"
val nd4jNativePlatformVersion = "1.0.0-M2.1"
val nd4jApiVersion = "1.0.0-M2.1"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.slf4j" % "slf4j-api" % slf4jLoggerVersion,
  "com.typesafe" % "config" % typeSafeConfigVersion,
  "com.knuddels" % "jtokkit" % jTokkitVersion,

  "org.apache.hadoop" % "hadoop-client" % hadoopClientVersion,
  "org.apache.hadoop" % "hadoop-common" % hadoopCommonVersion,
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopHdfsVersion,
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopMapReduceClientCoreVersion,
  "org.apache.hadoop" % "hadoop-yarn-client" % hadoopYarnClientVersion,
  "org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % hadoopMapReduceJobClientVersion,
  "org.scalanlp" %% "breeze" % breezeVersion,
  // Core DeepLearning4J dependency
  "org.deeplearning4j" % "deeplearning4j-core" % deepLearning4jVersion,
  "org.nd4j" % "nd4j-api" % nd4jApiVersion,
  "org.nd4j" % "nd4j-native" % "1.0.0-M2.1" classifier "macosx-x86_64",
  // ND4J backend (for CPU execution)
  "org.nd4j" % "nd4j-native-platform" % nd4jNativePlatformVersion,
  // DeepLearning4J NLP module
  "org.deeplearning4j" % "deeplearning4j-nlp" % deepLearning4jNLP,
)

// Main Class Configuration
Compile / mainClass := Option("edu.uic.llmforge.Main")
run / mainClass := Option("edu.uic.llmforge.Main")

// Merging Strategies for Assembly
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}