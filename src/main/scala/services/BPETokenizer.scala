package edu.uic.llmforge
package services

import edu.uic.llmforge.model.VectorGenerator
import edu.uic.llmforge.utils.ConfigUtil
import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapreduce.{Mapper, Reducer}
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters.*

object BPETokenizer {

  class BPEMapper extends Mapper[LongWritable, Text, Text, IntWritable] {
    val one = new IntWritable(1)
    val wordText = new Text()
    private val logger = LoggerFactory.getLogger(classOf[BPEMapper])

    override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, Text, IntWritable]#Context): Unit = {
      val line = value.toString.toLowerCase

      try {
        // Tokenize the line into words
        val words = line.split("\\W+").filter(_.nonEmpty)

        words.foreach { word =>
          wordText.set(word)
          context.write(wordText, one)
        }

        logger.info(s"Successfully processed line: $line")

      } catch {
        case e: Exception =>
          logger.error(s"Error processing line: $line, Exception: ${e.getMessage}", e)
      }
    }
  }

  // Reducer class
  class BPEReducer extends Reducer[Text, IntWritable, Text, Text] {
    private val logger = LoggerFactory.getLogger(classOf[BPEReducer])
    private val collectedTokens = scala.collection.mutable.ListBuffer[Int]()
    private val embeddingOutputFile = s"${ConfigUtil.finalConfig.embeddingOutputPath}" // Path where embeddings will be saved
    private val encoder = new Encoder()

    override def reduce(key: Text, values: java.lang.Iterable[IntWritable], context: Reducer[Text, IntWritable, Text, Text]#Context): Unit = {
      try {
        // Sum the counts
        val sum = values.asScala.foldLeft(0)(_ + _.get())

        // Apply Byte Pair Encoding
        val word = key.toString
        val tokens = encoder.encodeLine(word)

        // Collect tokens for embedding generation
        collectedTokens ++= tokens

        // Prepare output value
        val tokensStr = tokens.mkString("[", " ", "]")
        val outputValue = s"$tokensStr,$sum"

        context.write(key, new Text(outputValue))
        logger.info(s"Successfully processed word: $word with tokens: $tokensStr")

      } catch {
        case e: Exception =>
          logger.error(s"Error reducing key: $key, Exception: ${e.getMessage}", e)
      }
    }

    override def cleanup(context: Reducer[Text, IntWritable, Text, Text]#Context): Unit = {
      // Train the embedding model using the collected tokens
      if (collectedTokens.nonEmpty) {
        println("Training embeddings using the collected tokens...")
        val uniqueTokens = collectedTokens.distinct.toSeq
        VectorGenerator.trainAndSaveEmbeddings(uniqueTokens, windowSize = 3, stride = 1, outputFileName = embeddingOutputFile)
        println(s"Embeddings saved to $embeddingOutputFile")
      }
    }
  }

}
