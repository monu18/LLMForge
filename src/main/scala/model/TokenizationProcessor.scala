package edu.uic.llmforge
package model

import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import scala.io.Source
import scala.collection.mutable.ListBuffer

// Main class to handle tokenization and labeling
class TokenizationProcessor {

  // Method to read the output file and create tokenized sentences and labels
  def readTokenizedData(filePath: String): (INDArray, INDArray) = {
    val tokenizedSentences = ListBuffer[Array[Int]]()
    val labels = ListBuffer[Array[Int]]()

    // Read the file and parse each line
    for (line <- Source.fromFile(filePath).getLines()) {
      val parts = line.split("\t")  // Assuming the format is "sentence [tokens]"

      // Parse tokenized sentence
      if (parts.length == 2) {
        val sentenceTokens = parts(1).trim.stripPrefix("[").stripSuffix("]").split(",").map(_.trim.toInt)
        tokenizedSentences += sentenceTokens

        // Create labels by shifting the sentence tokens
        if (sentenceTokens.length > 1) {
          val labelTokens = sentenceTokens.tail // Shift left to create labels
          labels += labelTokens
        }
      }
    }

    // Convert to NDArray
    val inputFeatures: INDArray = Nd4j.create(tokenizedSentences.toArray)
    val outputLabels: INDArray = Nd4j.create(labels.toArray)

    (inputFeatures, outputLabels)
  }
}

// Main application object to run the processing
object TokenizationApp {
  def main(args: Array[String]): Unit = {
    // Create an instance of TokenizationProcessor
    val processor = new TokenizationProcessor()

    // Define the path to your output file
    val filePath = "path/to/your/output/part-r-00000"

    // Read tokenized data and get INDArray inputs and labels
    val (inputFeatures, outputLabels) = processor.readTokenizedData(filePath)

    // Print the results to verify
    println(s"Tokenized Sentences: \n$inputFeatures")
    println(s"Labels: \n$outputLabels")

    // Here you can add your model training logic using inputFeatures and outputLabels
  }
}
