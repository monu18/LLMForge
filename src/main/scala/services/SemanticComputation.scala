package edu.uic.llmforge
package services

import utils.{ConstantsUtil, SimilarityUtil}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{IntWritable, Text}
import org.apache.hadoop.mapreduce.{Job, Mapper, Reducer}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat

import scala.jdk.CollectionConverters.*
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.api.ndarray.INDArray

import scala.collection.mutable.ArrayBuffer

object SemanticComputation {

  def main(args: Array[String]): Unit = {

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
    runJob(conf3, embeddingCsv, semanticsOutputPath)
  }

  def runJob(conf: Configuration, inputPath: String, outputPath: String): Unit = {
    val job = Job.getInstance(conf, "Semantics Job")
    job.setJarByClass(this.getClass)

    job.setMapperClass(classOf[SemanticMapper])
    job.setReducerClass(classOf[SemanticReducer])

    job.setMapOutputKeyClass(classOf[Text])
    job.setMapOutputValueClass(classOf[Text])

    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[Text])

    FileInputFormat.addInputPath(job, new Path(inputPath))
    FileOutputFormat.setOutputPath(job, new Path(outputPath))

    if (job.waitForCompletion(true)) {
      println("Job completed successfully.")
    } else {
      println("Job failed.")
    }
  }

  class SemanticMapper extends Mapper[Object, Text, Text, Text] {

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, Text]#Context): Unit = {
      val line = value.toString.trim

      // Splitting the CSV line while handling potential commas within quoted fields
      val parts = splitCSV(line)

      // Check if the line contains at least a word and one embedding value
      if (parts.length > 2) {
        val word = parts(1).trim // The third column is the word
        val embedding = parts.drop(2).mkString(",") // Join all embedding values into a single string
        context.write(new Text(word), new Text(embedding))
      }
    }

    // Helper method to split CSV lines while handling quoted commas
    private def splitCSV(line: String): Array[String] = {
      val buffer = ArrayBuffer[String]()
      val current = new StringBuilder
      var inQuotes = false

      line.foreach {
        case '"' => inQuotes = !inQuotes // Toggle inQuotes flag
        case ',' if !inQuotes =>
          buffer += current.toString().trim // Add the value
          current.clear() // Clear for the next value
        case char => current.append(char)
      }

      buffer += current.toString().trim // Add the last value
      buffer.toArray
    }
  }

  // Reducer class to compute cosine similarities
  class SemanticReducer extends Reducer[Text, Text, Text, Text] {
    // Store all word embeddings
    private val wordEmbeddings = scala.collection.mutable.Map[String, INDArray]()

    override def reduce(key: Text, values: java.lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = {
      // Collect the current word's embeddings
      val embeddingArray = values.asScala.toSeq.flatMap(value => value.toString.split(",").map(_.toDouble))

      if (embeddingArray.nonEmpty) {
        val embeddingVector = Nd4j.create(embeddingArray.toArray)
        wordEmbeddings.put(key.toString, embeddingVector)
      }
    }

    // Cleanup method runs after all reduce calls are done
    override def cleanup(context: Reducer[Text, Text, Text, Text]#Context): Unit = {

      // Now we have all word embeddings, perform pairwise similarity calculations
      wordEmbeddings.foreach { case (word1, vector1) =>
        // Compute cosine similarity with all other embeddings
        val similarities = wordEmbeddings.collect {
          case (word2, vector2) if word1 != word2 =>
            val similarity = SimilarityUtil.cosineSimilarity(vector1.toDoubleVector, vector2.toDoubleVector)
            (word2, similarity)
        }

        // Get the top N most similar words
        val topSimilarWords = similarities.toSeq.sortBy(-_._2).take(ConstantsUtil.MOST_SIMILAR_WORDS_BATCH_SIZE)

        // Convert to the desired output format
        val similarWordsStr = topSimilarWords.map { case (word, score) =>
          s"$word:$score"
        }.mkString("; ")

        // Write the output
        context.write(new Text(word1), new Text(similarWordsStr))
      }
    }
  }

}
