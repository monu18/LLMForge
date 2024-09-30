package edu.uic.llmforge
package services

import utils.FileUtil
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.{Encoding, EncodingRegistry, IntArrayList, ModelType}

class Decoder extends Tokenizer {
  private val logger = LoggerFactory.getLogger(getClass)
  private val encodingRegistry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
  private val encoding: Encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_4)

  // Method to decode a single encoded line
  def decodeLine(encodedStr: String): String = {
    val encodedList = parseEncodedStringToIntArrayList(encodedStr)
    encoding.decode(encodedList) // Decode back to String
  }

  override def process(inputPath: String, outputPath: String): Unit = {
    val result = Try {
      val lines = FileUtil.readFile(inputPath) // Read encoded file

      val decodedLines = lines.map { encodedLine =>
        decodeLine(encodedLine) // Call decodeLine with a single argument
      }

      FileUtil.writeFile(outputPath, decodedLines) // Write decoded data to output file
    }

    result match {
      case Success(_) =>
        logger.info(s"Successfully decoded the file: $inputPath")
      case Failure(exception) =>
        logger.error(s"Error occurred while decoding: ${exception.getMessage}", exception)
    }
  }

  // Helper function to parse string representation of IntArrayList back to IntArrayList
  private def parseEncodedStringToIntArrayList(encodedStr: String): IntArrayList = {
    val intList = new IntArrayList()

    Try {
      val cleanedStr = encodedStr.stripPrefix("[").stripSuffix("]")
      val intArray = cleanedStr.split(",").map(_.trim.toInt)
      intArray.foreach(intList.add)
    } match {
      case Success(_) =>
        logger.info("Parsing successful")
      case Failure(exception) =>
        logger.error(s"Error occurred while parsing encoded string '$encodedStr': ${exception.getMessage}", exception)
    }

    intList
  }
}