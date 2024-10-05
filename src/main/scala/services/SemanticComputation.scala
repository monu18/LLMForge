package edu.uic.llmforge
package services

import utils.{ConstantsUtil, SimilarityUtil}

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.{Mapper, Reducer}
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

object SemanticComputation {

  class SemanticMapper extends Mapper[Object, Text, Text, Text] {

    // Store the word embeddings for the current batch of records
    private val wordEmbeddings = scala.collection.mutable.Map[String, INDArray]()

    override def map(key: Object, value: Text, context: Mapper[Object, Text, Text, Text]#Context): Unit = {
      val line = value.toString.trim
      val parts = splitCSV(line)

      if (parts.length > 2) {
        val word = parts(1).trim // The third column is the word
        val embedding = parts.drop(2).map(_.toDouble)
        val embeddingVector = Nd4j.create(embedding.toArray)

        wordEmbeddings.put(word, embeddingVector)

        // Perform partial similarity calculation for the batch
        wordEmbeddings.foreach { case (otherWord, otherEmbedding) =>
          if (word != otherWord) {
            val similarity = SimilarityUtil.cosineSimilarity(embeddingVector.toDoubleVector, otherEmbedding.toDoubleVector)
            context.write(new Text(s"$word |"), new Text(s"$otherWord:$similarity"))
            context.write(new Text(s"$otherWord |"), new Text(s"$word:$similarity")) // Symmetric similarities
          }
        }
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

  class SemanticReducer extends Reducer[Text, Text, Text, Text] {

    // Reducer to collect and aggregate similarities
    override def reduce(key: Text, values: java.lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = {
      val similarities = values.asScala.map(_.toString).toSeq

      // Collect all similarity values
      val similarityMap = similarities.map { similarityStr =>
        val Array(word, score) = similarityStr.split(":")
        (word, score.toDouble)
      }

      // Sort by highest similarity and get top N similar words
      val topSimilarWords = similarityMap.toSeq.sortBy(-_._2).take(ConstantsUtil.MOST_SIMILAR_WORDS_BATCH_SIZE)

      // Convert to the desired output format
      val similarWordsStr = topSimilarWords.map { case (word, score) =>
        s"$word:$score"
      }.mkString("; ")

      // Write the output
      context.write(key, new Text(similarWordsStr))
    }
  }
}
