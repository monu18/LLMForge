package edu.uic.llmforge
package utils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import scala.io.Source
import java.io._

class ShardUtil {

  // Method to delete existing shard files for local filesystem
  private def deleteExistingShardsLocal(outputDir: String): Unit = {
    val dir = new File(outputDir)
    if (dir.exists() && dir.isDirectory) {
      // List all files in the directory and delete those that match the shard pattern
      dir.listFiles().filter(_.getName.startsWith("shard_")).foreach(_.delete())
    }
  }

  // Method to delete existing shard files for HDFS
  private def deleteExistingShardsHDFS(outputDir: String, fs: FileSystem): Unit = {
    val outputPath = new Path(outputDir)
    if (fs.exists(outputPath)) {
      fs.delete(outputPath, true)
    }
  }

  private def cleanText(line: String): String = {
    // Regular expression to keep only words, numeric, alphanumeric, and apostrophes
    line.replaceAll("[^\\w\\s]", "").trim
  }

  // Local sharding method
  private def shardLocal(inputFilePath: String, outputDir: String, shardSize: Int): Unit = {
    deleteExistingShardsLocal(outputDir)
    val inputFile = Source.fromFile(inputFilePath)
    val lines = inputFile.getLines()

    var shardCount = 0
    var currentShard = new PrintWriter(new File(s"$outputDir/shard_$shardCount.txt"))
    var lineCount = 0

    for (line <- lines) {
      val cleanedLine = cleanText(line)
      if (lineCount >= shardSize) {
        currentShard.close()
        shardCount += 1
        currentShard = new PrintWriter(new File(s"$outputDir/shard_$shardCount.txt"))
        lineCount = 0
      }
      currentShard.println(cleanedLine)
      lineCount += 1
    }
    currentShard.close()
    inputFile.close()
  }

  // HDFS sharding method
  private def shardHDFS(inputFilePath: String, outputDir: String, shardSize: Int, fs: FileSystem): Unit = {
    deleteExistingShardsHDFS(outputDir, fs)

    val inputPath = new Path(inputFilePath)
    val inputStream = fs.open(inputPath)
    val lines = Source.fromInputStream(inputStream).getLines()

    var shardCount = 0
    var currentShard = fs.create(new Path(s"$outputDir/shard_$shardCount.txt"))
    var lineCount = 0

    for (line <- lines) {
      val cleanedLine = cleanText(line)
      if (lineCount >= shardSize) {
        currentShard.close()
        shardCount += 1
        currentShard = fs.create(new Path(s"$outputDir/shard_$shardCount.txt"))
        lineCount = 0
      }
      currentShard.writeBytes(s"""$cleanedLine
""")
      lineCount += 1
    }
    currentShard.close()
    inputStream.close()
  }

  def shardText(): Unit = {
    // Load paths from the configuration
    val inputDatasetPath: String = s"${ConfigUtil.finalConfig.inputDatasetPath}"
    val shardsDirectory: String = s"${ConfigUtil.finalConfig.shardsDirectory}"
    val shardSize = ConstantsUtil.SHARD_SIZE

    val conf = new Configuration()

    if (inputDatasetPath.startsWith("/user/hadoop/")) {
      conf.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host
      val fs = FileSystem.get(conf)
      shardHDFS(inputDatasetPath, shardsDirectory, shardSize, fs)
    } else {
      conf.set("fs.defaultFS", "file:///")
      shardLocal(inputDatasetPath, shardsDirectory, shardSize)
    }
  }
}
