package edu.uic.llmforge

import services.*
import services.BPETokenizer.{BPEMapper, BPEReducer}
import utils.{ConfigUtil, ShardUtil}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat

@main
def main(args: String*): Unit = {

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
  val encodingDirectory: String = s"${ConfigUtil.finalConfig.encodingDirectory}"
  val shardsDirectory: String = s"${ConfigUtil.finalConfig.shardsDirectory}"
  val embeddingOutputPath: String = s"${ConfigUtil.finalConfig.embeddingOutputPath}"

  val conf = new Configuration()

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
    println("Job completed successfully.")
  } else {
    println("Job failed.")
  }
  
//  val conf = new Configuration()
//  // Ensure configuration paths are correct
////  conf.addResource(new Path("/opt/homebrew/Cellar/hadoop/3.3.6/etc/hadoop/core-site.xml"))
////  conf.addResource(new Path("/opt/homebrew/Cellar/hadoop/3.3.6/etc/hadoop/hdfs-site.xml"))
////  conf.addResource(new Path("/opt/homebrew/Cellar/hadoop/3.3.6/etc/hadoop/mapred-site.xml"))
////  conf.addResource(new Path("/opt/homebrew/Cellar/hadoop/3.3.6/etc/hadoop/yarn-site.xml"))
//
//  // Set the correct Hadoop filesystem (usually it defaults to your local file system if not on HDFS)
//  val fs = FileSystem.get(conf)
//  val outputPath = new Path("/Users/monu/IdeaProjects/LLMForge/src/main/resources/encodingoutput")
//
//  // Check if output path already exists, and delete it if so
//  if (fs.exists(outputPath)) {
//    fs.delete(outputPath, true) // 'true' indicates recursive delete
//  }
//
//  // Encoding Job
//  val encodingJob = Job.getInstance(conf, "Encoding Job")
//  encodingJob.setJarByClass(this.getClass)
//  
//  encodingJob.setMapperClass(classOf[EncodingMapper])
//  encodingJob.setReducerClass(classOf[EncodingReducer])
//
//  encodingJob.setMapOutputKeyClass(classOf[Text])
//  encodingJob.setMapOutputValueClass(classOf[IntWritable])
//  
//  encodingJob.setOutputKeyClass(classOf[Text])
//  encodingJob.setOutputValueClass(classOf[Text])
//  
//  FileInputFormat.addInputPath(encodingJob, new Path("/Users/monu/IdeaProjects/LLMForge/src/main/resources/output/shards"))
//  FileOutputFormat.setOutputPath(encodingJob, new Path("/Users/monu/IdeaProjects/LLMForge/src/main/resources/encodingoutput"))
//
//  // Run the encoding job
//  if (encodingJob.waitForCompletion(true)) {
//    println("Encoding job completed successfully.")
//  }

  
    // Decoding Job
//    val decodingJob = Job.getInstance(new Configuration(), "Decoding Job")
//    decodingJob.setJarByClass(getClass)
//    decodingJob.setMapperClass(classOf[DecodingMapper]) // Assume you have defined this mapper
//    decodingJob.setOutputKeyClass(classOf[Text])
//    decodingJob.setOutputValueClass(classOf[Text])
//    FileInputFormat.addInputPath(decodingJob, new Path("/Users/monu/IdeaProjects/LLMForge/src/main/resources/encodingoutput/part-r-00000"))
//    FileOutputFormat.setOutputPath(decodingJob, new Path("/Users/monu/IdeaProjects/LLMForge/src/main/resources/decodingoutput"))
//
//    // Run the decoding job
//    if (decodingJob.waitForCompletion(true)) {
//      println("Decoding job completed successfully.")
//    } else {
//      println("Decoding job failed.")
//    }
//  } else {
//    println("Encoding job failed.")
//  }
}
