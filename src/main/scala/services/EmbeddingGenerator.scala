package edu.uic.llmforge
package services

import edu.uic.llmforge.utils.{ConfigUtil, ConstantsUtil}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.{Mapper, Reducer}
import org.apache.log4j.Logger
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

object EmbeddingGenerator {

  class EmbeddingMapper extends Mapper[LongWritable, Text, Text, Text] {

    private val logger = LoggerFactory.getLogger(classOf[EmbeddingMapper])
    private val collectedTokens = ListBuffer[Int]() // Token buffer
    private val batchSize = ConstantsUtil.TOKEN_BATCH_SIZE // Process tokens in batches (adjust size as needed)

    // The map method collects tokens and processes them in batches
    override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
      val line = value.toString.trim
      val tokens = line.split("\\s+").map(_.toInt) // Convert tokens to integers
      collectedTokens ++= tokens // Collect tokens from this line

      // Process tokens in batches
      if (collectedTokens.sizeIs >= batchSize) {
        processBatch(context)
      }
    }

    // The cleanup method is called once at the end of processing the shard
    override def cleanup(context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
      logger.info("Mapper task finished. Cleanup called.")
      // Process any remaining tokens that didn't form a full batch
      if (collectedTokens.nonEmpty) {
        processBatch(context)
      }
    }

    // Process the current batch of tokens and clear the buffer
    private def processBatch(context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
      // Generate embeddings for the current batch of tokens
      val embeddings: Map[Int, INDArray] = EmbeddingPreprocessor.generateEmbeddingsForTokens(collectedTokens.toSeq, windowSize = ConstantsUtil.WINDOW_SIZE, stride = ConstantsUtil.STRIDE)

      // Emit each token and its corresponding embedding
      embeddings.foreach { case (token, embeddingVector) =>
        val embeddingStr = embeddingVector.toDoubleVector.mkString(",")
        context.write(new Text(token.toString), new Text(embeddingStr))
      }

      // Clear the token buffer after processing the batch
      collectedTokens.clear()
    }
  }



  class EmbeddingReducer extends Reducer[Text, Text, Text, Text] {
    private val logger = LoggerFactory.getLogger(classOf[EmbeddingReducer])
    val encoder = new Encoder
    // Accumulate tokens from the shard
    private val collectedEmbeddings = ListBuffer[String]()
    
    override def reduce(key: Text, values: java.lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = {
      val embeddingVectors = values.asScala.map { value =>
        val vectorArray = value.toString.split(",").map(_.toDouble)
        Nd4j.create(vectorArray)
      }.toList

      // Compute the average of these vectors
      val sumVector = embeddingVectors.reduce(_ add _)
      val averageVector = sumVector.div(embeddingVectors.size)

      // Convert the average vector to a string representation
      val tokenID = key.toString.toInt
      val tokenWord = encoder.decode(Seq(tokenID))
      val embeddingStr = averageVector.toDoubleVector.mkString(",")

      collectedEmbeddings += s"$tokenID,$tokenWord,$embeddingStr"

      context.write(key, new Text(averageVector.toDoubleVector.mkString(",")))
    }

    // Cleanup method in Reducer
    override def cleanup(context: Reducer[Text, Text, Text, Text]#Context): Unit = {
      logger.info("Reducer task finished. Cleanup called.")
      val embeddingCsv = new Path("src/main/resources/output/embeddings.csv")
      val fs = embeddingCsv.getFileSystem(context.getConfiguration)
      val outputStream = fs.create(embeddingCsv, true)

      collectedEmbeddings.foreach(token => {
        outputStream.writeBytes(s"""$token
""")
      })

      outputStream.close()
    }
  }
}
