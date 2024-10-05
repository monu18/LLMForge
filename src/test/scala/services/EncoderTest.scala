package edu.uic.llmforge.services

import scala.collection.mutable
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.jdk.CollectionConverters._

//// Mock implementation of FileUtil for testing
//object MockFileUtil {
//  private val fileStorage = mutable.Map[String, Seq[String]]()
//
//  def readFile(path: String): Seq[String] = {
//    fileStorage.getOrElse(path, Seq.empty)
//  }
//
//  def writeFile(path: String, data: Seq[String]): Unit = {
//    fileStorage(path) = data // Store data in the mock storage
//  }
//
//  def clear(): Unit = {
//    fileStorage.clear() // Clear mock storage between tests
//  }
//}

class EncoderTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private val encoder = new Encoder() // Create an instance of Encoder

  // Clear mock file storage before all tests
  override def beforeAll(): Unit = {
    MockFileUtil.clear() // Clear mock storage before running tests
    super.beforeAll()
  }

  test("process should encode lines from the input file and write to the output file") {
    // Prepare test input
    val inputPath = "src/main/resources/input/dataTest.txt"
    val outputPath = "src/main/resources/output/testOutput.txt"

    // Write input data to the actual file system
    import java.nio.file.{Files, Paths}
    import java.nio.charset.StandardCharsets
    Files.write(Paths.get(inputPath), "ability access".getBytes(StandardCharsets.UTF_8))

    // Call process method
    encoder.process(inputPath, outputPath)

    // Verify output by reading from the actual output file
    val output = Files.readAllLines(Paths.get(outputPath), StandardCharsets.UTF_8).asScala.toList

    // Verify the result
    val expectedOutput = List("2968 2680") // Adjust according to your actual encoding logic
    output shouldEqual expectedOutput
  }

  test("encodeLine should encode a single line correctly") {
    // Test a single line encoding
    val line = "ability"
    val expectedTokens = Seq(2968) // Replace with actual expected tokens based on encoding logic

    val tokens = encoder.encodeLine(line)
    tokens shouldEqual expectedTokens
  }

  test("decode should decode tokens back to the original string") {
    // Test decoding of tokens
    val tokens = Seq(2968, 2680) // Tokens that encode "ability access"
    val expectedString = "ability access" // Replace with the actual expected decoded string

    val decodedString = encoder.decode(tokens)
    decodedString shouldEqual expectedString
  }

  test("createInputOutputPairs should create pairs correctly") {
    val tokens = Seq(1, 2, 3, 4, 5)
    val windowSize = 2
    val stride = 1

    val expectedPairs = Seq(
      (Array(1, 2), 3),
      (Array(2, 3), 4),
      (Array(3, 4), 5)
    )

    val pairs = encoder.createInputOutputPairs(tokens, windowSize, stride)

    // Custom equality check for arrays using deep comparison
    pairs.zip(expectedPairs).foreach { case ((arr1, token1), (arr2, token2)) =>
      arr1.sameElements(arr2) shouldBe true // Compare arrays by content
      token1 shouldEqual token2 // Compare tokens normally
    }
  }
}
