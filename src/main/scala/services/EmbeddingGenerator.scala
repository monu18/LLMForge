package edu.uic.llmforge
package services

import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.{Mapper, Reducer}
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

object EmbeddingGenerator {

  class EmbeddingMapper extends Mapper[LongWritable, Text, Text, Text] {
    // Accumulate tokens from the shard
    private val collectedTokens = ListBuffer[Int]()

    // The map method now only collects tokens
    override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
      val line = value.toString.trim
      val tokens = line.split("\\s+").map(_.toInt) // Convert tokens to integers
      collectedTokens ++= tokens // Collect all tokens from this shard
    }

    // The cleanup method is called once at the end of processing the shard
    override def cleanup(context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
      if (collectedTokens.nonEmpty) {
        // Generate embeddings using the EmbeddingGenerator for the entire shard's tokens
        val embeddings: Map[Int, INDArray] = EmbeddingPreprocessor.generateEmbeddingsForTokens(collectedTokens.toSeq, windowSize = 3, stride = 1)

        // Emit each token and its corresponding embedding
        embeddings.foreach { case (token, embeddingVector) =>
          val embeddingStr = embeddingVector.toDoubleVector.mkString(",")
          context.write(new Text(token.toString), new Text(embeddingStr))
        }
      }
    }
  }


  class EmbeddingReducer extends Reducer[Text, Text, Text, Text] {
    val encoder = new Encoder

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

      // Write the final averaged embedding
      EmbeddingPreprocessor.saveEmbeddingToCSV(tokenID, tokenWord, embeddingStr)

      context.write(key, new Text(averageVector.toDoubleVector.mkString(",")))
    }
  }
}
