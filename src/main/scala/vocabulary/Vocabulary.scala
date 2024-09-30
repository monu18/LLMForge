package edu.uic.llmforge
package vocabulary


import scala.collection.mutable
import java.io.{BufferedWriter, FileWriter}
import scala.util.{Failure, Success, Try}

object Vocabulary {
  private val wordFrequency = mutable.Map[String, Long]().withDefaultValue(0L)
  private val wordTokenMap = mutable.Map[String, String]()

  // Function to process the vocabulary
  def processVocabulary(textCorpus: String, encodedCorpus: String): Unit = {
    val words = textCorpus.split("\\s+")
    // Extract tokens from the encodedCorpus, removing brackets and spaces
    val tokens = encodedCorpus.stripPrefix("[").stripSuffix("]").split(",").map(_.trim)
    

    // Ensure we have the same number of words and tokens
    if (words.length != tokens.length) {
      throw new IllegalArgumentException("Number of words and tokens do not match")
    }

    // Update word frequency and map word to its token
    words.zip(tokens).foreach { case (word, token) =>
      wordFrequency(word) += 1
      wordTokenMap.getOrElseUpdate(word, token) // Store token if not already stored
    }
  }

  // Function to write the vocabulary to a CSV file
  def writeVocabularyToCSV(filePath: String): Unit = {
    val fileWriter = new BufferedWriter(new FileWriter(filePath))
    fileWriter.write("Word,Token,Frequency\n")

    wordFrequency.foreach { case (word, frequency) =>
      val token = wordTokenMap(word)
      fileWriter.write(s"$word,$token,$frequency\n")
    }
    fileWriter.close()
  }
}

