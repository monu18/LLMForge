package edu.uic.llmforge
package services

import utils.FileUtil
import scala.util.{Try, Success, Failure}
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.{Encoding, EncodingRegistry, IntArrayList, ModelType}
import org.slf4j.LoggerFactory

class Encoder extends Tokenizer {
  private val logger = LoggerFactory.getLogger(getClass)
  private val encodingRegistry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
  private val encoding: Encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_4)
  
  override def process(inputPath: String, outputPath: String): Unit = {
    val result = Try {
      val lines = FileUtil.readFile(inputPath) // Read input file

      val encodedLines = lines.map { line =>
        val tokens = encodeLine(line)
        tokens.mkString(" ") // Convert Seq[Int] to a single string with space-separated tokens
      }

      FileUtil.writeFile(outputPath, encodedLines) // Write encoded data to output file
    }

    result match {
      case Success(_) =>
        logger.info(s"Successfully processed the file: $inputPath")
      case Failure(exception) =>
        logger.error(s"Error occurred: ${exception.getMessage}", exception)
    }
  }

  // Method to encode a single line of text
  def encodeLine(line: String): Seq[Int] = {
    try {
      val encoded: IntArrayList = encoding.encodeOrdinary(line) // Assuming `encoding` is properly initialized
      // Convert IntArrayList to Scala List[Int]
      val tokens = (0 until encoded.size()).map(i => encoded.get(i)).toList
      tokens

    } catch {
      case e: Exception =>
        logger.error(s"Error encoding line: $line, Exception: ${e.getMessage}", e)
        Seq.empty[Int] // Return an empty sequence if encoding fails
    }
  }

  // Method to decode tokens back to the original string
  def decode(tokens: Seq[Int]): String = {
    // Convert Scala Seq[Int] to IntArrayList
    val intArrayList = new IntArrayList()
    tokens.foreach(intArrayList.add)
    // Decode using IntArrayList
    encoding.decode(intArrayList)
  }
  def createInputOutputPairs(tokens: Seq[Int], windowSize: Int, stride: Int): Seq[(Array[Int], Int)] = {
    tokens.sliding(windowSize + 1, stride).map { window =>
      val inputSeq = window.take(windowSize).toArray
      val targetToken = window.last
      (inputSeq, targetToken)
    }.toSeq
  }
}
