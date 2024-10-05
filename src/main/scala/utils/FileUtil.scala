package edu.uic.llmforge
package utils

import java.io.{File, PrintWriter}
import scala.io.Source

object FileUtil {
  // Method to read lines from a file
  def readFile(inputPath: String): List[String] = {
    val source = Source.fromFile(inputPath)
    try source.getLines().toList finally source.close()
  }

  // Method to write lines to a file
  def writeFile(outputPath: String, lines: List[String]): Unit = {
    val pw = new PrintWriter(new File(outputPath))
    try lines.foreach(pw.println) finally pw.close()
  }
}
