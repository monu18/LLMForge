package edu.uic.llmforge

import services._
import services.BPETokenizer.{BPEMapper, BPEReducer}
import utils.{ConfigUtil, ShardUtil}

import edu.uic.llmforge.services.EmbeddingGenerator.{EmbeddingMapper, EmbeddingReducer}
import edu.uic.llmforge.services.SemanticComputation.{SemanticMapper, SemanticReducer}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.slf4j.{Logger, LoggerFactory}

object HW1 {

  // Create a logger instance for this class
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    // Check if arguments are provided and initialize configuration
    if (args.nonEmpty) {
      logger.info("Initializing configuration with provided arguments: {}", args.mkString(", "))
      ConfigUtil.initializeConfig(args.toList)
    } else {
      logger.warn("No command-line arguments provided. Using default configuration.")
    }

    // shard text corpus
    val shardUtil = new ShardUtil()
    logger.info("Sharding the text corpus...")
    shardUtil.shardText()

    runHadoopJobs()
  }

  def runHadoopJobs(): Unit = {

    // Load paths from the configuration
    val inputPath: String = ConfigUtil.finalConfig.inputDatasetPath
    val shardsDirectory: String = ConfigUtil.finalConfig.shardsDirectory
    val encodingDirectory: String = ConfigUtil.finalConfig.encodingDirectory
    val tokenOutputPath: String = ConfigUtil.finalConfig.tokenOutputPath
    val embeddingOutputPath: String = ConfigUtil.finalConfig.embeddingOutputPath
    val embeddingCsvPath: String = ConfigUtil.finalConfig.embeddingCsvPath
    val semanticsOutputPath: String = ConfigUtil.finalConfig.semanticsOutputPath

    logger.info("Input paths loaded from configuration.")

    val conf = new Configuration()

    // Update the configuration to handle S3 paths if using EMR (input/output paths in S3 start with "s3://")
    if (inputPath.startsWith("hdfs")) {
      logger.info("Using HDFS paths for input.")
    } else if (inputPath.startsWith("/user/hadoop/")) {
      logger.info("Using local HDFS for input.")
      conf.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host if using HDFS
    } else {
      logger.info("Using local filesystem for input.")
      conf.set("fs.defaultFS", "file:///")
    }

    val fs = FileSystem.get(conf)
    val encodingDirectoryPath = new Path(encodingDirectory)

    // Check if output path already exists, and delete it if so
    if (fs.exists(encodingDirectoryPath)) {
      logger.warn("Output path {} exists. Deleting it...", encodingDirectory)
      fs.delete(encodingDirectoryPath, true) // 'true' indicates recursive delete
    }

    val wordCountJob = Job.getInstance(conf, "Word Count")
    wordCountJob.setJarByClass(this.getClass)

    wordCountJob.setMapperClass(classOf[BPEMapper])
    wordCountJob.setReducerClass(classOf[BPEReducer])

    wordCountJob.setMapOutputKeyClass(classOf[Text])
    wordCountJob.setMapOutputValueClass(classOf[IntWritable])

    wordCountJob.setOutputKeyClass(classOf[Text])
    wordCountJob.setOutputValueClass(classOf[Text])

    FileInputFormat.addInputPath(wordCountJob, new Path(shardsDirectory))
    FileOutputFormat.setOutputPath(wordCountJob, encodingDirectoryPath)

    logger.info("Starting WordCount job...")
    if (wordCountJob.waitForCompletion(true)) {
      logger.info("WordCountJob completed successfully.")

      // Next Embedding Mapper Reducer
      val conf2 = new Configuration()
      if (inputPath.startsWith("hdfs")) {
        logger.info("Using HDFS for embedding input.")
      } else if (inputPath.startsWith("/user/hadoop/")) {
        logger.info("Using local HDFS for embedding input.")
        conf2.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host if using HDFS
      } else {
        logger.info("Using local filesystem for embedding input.")
        conf2.set("fs.defaultFS", "file:///")
      }

      val fs2 = FileSystem.get(conf2)
      val embeddingDirectoryPath = new Path(embeddingOutputPath)

      if (fs2.exists(embeddingDirectoryPath)) {
        logger.warn("Embedding output path {} exists. Deleting it...", embeddingOutputPath)
        fs2.delete(embeddingDirectoryPath, true)
      }

      val embeddingJob = Job.getInstance(conf2, "Embedding Vector Generator")
      embeddingJob.setJarByClass(this.getClass)

      embeddingJob.setMapperClass(classOf[EmbeddingMapper])
      embeddingJob.setReducerClass(classOf[EmbeddingReducer])

      embeddingJob.setMapOutputKeyClass(classOf[Text])
      embeddingJob.setMapOutputValueClass(classOf[Text])

      embeddingJob.setOutputKeyClass(classOf[Text])
      embeddingJob.setOutputValueClass(classOf[Text])

      FileInputFormat.addInputPath(embeddingJob, new Path(tokenOutputPath))
      FileOutputFormat.setOutputPath(embeddingJob, embeddingDirectoryPath)

      logger.info("Starting Embedding job...")
      if (embeddingJob.waitForCompletion(true)) {
        logger.info("EmbeddingJob completed successfully.")

        val conf3 = new Configuration()
        if (inputPath.startsWith("hdfs")) {
          logger.info("Using HDFS for semantics input.")
        } else if (embeddingCsvPath.startsWith("/user/hadoop/")) {
          logger.info("Using local HDFS for semantics input.")
          conf3.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host if using HDFS
        } else {
          logger.info("Using local filesystem for semantics input.")
          conf3.set("fs.defaultFS", "file:///")
        }

        val fs3 = FileSystem.get(conf3)
        val semanticsDirectoryPath = new Path(semanticsOutputPath)

        if (fs3.exists(semanticsDirectoryPath)) {
          logger.warn("Semantics output path {} exists. Deleting it...", semanticsOutputPath)
          fs3.delete(semanticsDirectoryPath, true)
        }

        val semanticsJob = Job.getInstance(conf3, "Semantics Job")
        semanticsJob.setJarByClass(this.getClass)

        semanticsJob.setMapperClass(classOf[SemanticMapper])
        semanticsJob.setReducerClass(classOf[SemanticReducer])

        semanticsJob.setMapOutputKeyClass(classOf[Text])
        semanticsJob.setMapOutputValueClass(classOf[Text])

        semanticsJob.setOutputKeyClass(classOf[Text])
        semanticsJob.setOutputValueClass(classOf[Text])

        FileInputFormat.addInputPath(semanticsJob, new Path(embeddingCsvPath))
        FileOutputFormat.setOutputPath(semanticsJob, semanticsDirectoryPath)

        logger.info("Starting Semantics job...")
        if (semanticsJob.waitForCompletion(true)) {
          logger.info("SemanticsJob completed successfully.")
        } else {
          logger.error("SemanticsJob failed.")
        }

      } else {
        logger.error("EmbeddingJob failed.")
      }

    } else {
      logger.error("WordCountJob failed.")
    }
  }
}
