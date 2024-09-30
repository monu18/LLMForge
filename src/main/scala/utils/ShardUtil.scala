package edu.uic.llmforge
package utils

import java.io._
import scala.io.Source

class ShardUtil {

  // Method to delete existing shard files
  private def deleteExistingShards(outputDir: String): Unit = {
    val dir = new File(outputDir)
    if (dir.exists() && dir.isDirectory) {
      // List all files in the directory and delete those that match the shard pattern
      dir.listFiles().filter(_.getName.startsWith("shard_")).foreach(_.delete())
    }
  }

  private def cleanText(line: String): String = {
    // Regular expression to keep only words, numeric, alphanumeric, and apostrophes
    line.replaceAll("[^\\w\\s]", "").trim
  }

  private def shard(inputFilePath: String, outputDir: String, shardSize: Int): Unit = {

    // Delete existing shards before creating new ones
    deleteExistingShards(outputDir)

    val inputFile = Source.fromFile(inputFilePath)
    val lines = inputFile.getLines()
    var shardCount = 0
    var currentShard = new PrintWriter(new File(s"$outputDir/shard_$shardCount.txt"))
    var lineCount = 0

    for (line <- lines) {
      val cleanedLine = cleanText(line) // Clean the line during sharding
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

  def shardText(): Unit = {
    // Load paths from the configuration
    val inputDatasetPath: String = s"${ConfigUtil.finalConfig.inputDatasetPath}"
    val shardsDirectory: String = s"${ConfigUtil.finalConfig.shardsDirectory}"

    val shardSize = Constants.shardSize // Number of lines per shard
    shard(inputDatasetPath, shardsDirectory, shardSize)
  }
}

