package edu.uic.llmforge.services

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable

// Mock implementation of FileUtil for testing
object MockFileUtil {
  private val fileStorage = mutable.Map[String, Seq[String]]()

  def readFile(path: String): Seq[String] = {
    fileStorage.getOrElse(path, Seq.empty)
  }

  def writeFile(path: String, data: Seq[String]): Unit = {
    fileStorage(path) = data // Store data in the mock storage
  }

  def clear(): Unit = {
    fileStorage.clear() // Clear mock storage between tests
  }
}

class DecoderTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private val decoder = new Decoder() // Create an instance of Decoder

  // Clear mock file storage before all tests
  override def beforeAll(): Unit = {
    super.beforeAll()
    MockFileUtil.clear() // Clear mock storage before running tests
  }

  test("decodeLine should decode a single encoded line correctly") {
    val encodedLine = "[2968, 2680]"
    val expectedDecodedString = "ability access" // Expected decoded string for the encodedLine

    val decodedString = decoder.decodeLine(encodedLine)
    decodedString shouldEqual expectedDecodedString
  }
}
