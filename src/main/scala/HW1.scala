package edu.uic.llmforge

import services.*
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

object HW1 {
  def main(args: Array[String]): Unit = {

    // Check if arguments are provided and initialize configuration
    if (args.nonEmpty) {
      ConfigUtil.initializeConfig(args.toList)
    } else {
      println("No command-line arguments provided. Using default configuration.")
    }

    // shard text corpus
    val shardUtil = new ShardUtil()
    shardUtil.shardText()

    runHadoopJobs()
  }

  def runHadoopJobs(): Unit = {

    // Load paths from the configuration
    val inputPath: String = s"${ConfigUtil.finalConfig.inputDatasetPath}"
    val shardsDirectory: String = s"${ConfigUtil.finalConfig.shardsDirectory}"
    val encodingDirectory: String = s"${ConfigUtil.finalConfig.encodingDirectory}"
    val tokenOutputPath: String = s"${ConfigUtil.finalConfig.tokenOutputPath}"
    val embeddingOutputPath: String = s"${ConfigUtil.finalConfig.embeddingOutputPath}"

    val conf = new Configuration()
    if (inputPath.startsWith("/user/hadoop/")) {
      conf.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host
    } else {
      conf.set("fs.defaultFS", "file:///")
    }

    // Set the correct Hadoop filesystem (usually it defaults to your local file system if not on HDFS)
    val fs = FileSystem.get(conf)
    val encodingDirectoryPath = new Path(encodingDirectory)

    // Check if output path already exists, and delete it if so
    if (fs.exists(encodingDirectoryPath)) {
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

    if (wordCountJob.waitForCompletion(true)) {
      println("WordCountJob completed successfully.")
      // next Embedding Mapper Reducer
      val conf2 = new Configuration()
      if (inputPath.startsWith("/user/hadoop/")) {
        conf2.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host
      } else {
        conf2.set("fs.defaultFS", "file:///")
      }

      val fs2 = FileSystem.get(conf2)
      val embeddingDirectoryPath = new Path(embeddingOutputPath)

      // Check if output path already exists, and delete it if so
      if (fs2.exists(embeddingDirectoryPath)) {
        fs2.delete(embeddingDirectoryPath, true) // 'true' indicates recursive delete
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
      if (embeddingJob.waitForCompletion(true)) {
        println("EmbeddingJob completed successfully.")
        val embeddingCsv: String = "src/main/resources/output/embeddings.csv"
        val semanticsOutputPath: String = "src/main/resources/output/semantics"
        val conf3 = new Configuration()
        if (embeddingCsv.startsWith("/user/hadoop/")) {
          conf3.set("fs.defaultFS", "hdfs://localhost:9000") // Adjust this with your HDFS host
        } else {
          conf3.set("fs.defaultFS", "file:///")
        }
        val fs2 = FileSystem.get(conf3)
        val semanticsDirectoryPath = new Path(semanticsOutputPath)

        // Check if output path already exists, and delete it if so
        if (fs2.exists(semanticsDirectoryPath)) {
          fs2.delete(semanticsDirectoryPath, true) // 'true' indicates recursive delete
        }
        val job = Job.getInstance(conf, "Semantics Job")
        job.setJarByClass(this.getClass)

        job.setMapperClass(classOf[SemanticMapper])
        job.setReducerClass(classOf[SemanticReducer])

        job.setMapOutputKeyClass(classOf[Text])
        job.setMapOutputValueClass(classOf[Text])

        job.setOutputKeyClass(classOf[Text])
        job.setOutputValueClass(classOf[Text])

        FileInputFormat.addInputPath(job, new Path(embeddingCsv))
        FileOutputFormat.setOutputPath(job, new Path(semanticsOutputPath))

        if (job.waitForCompletion(true)) {
          println("Job completed successfully.")
        } else {
          println("Job failed.")
        }
        
      }
      else {
        println("EmbeddingJob failed.")
      }
    }
    else {
      println("Job failed.")
    }
  }
}


