package edu.uic.llmforge
package services

abstract class Tokenizer {
  // Abstract method signatures
  def process(inputPath: String, outputPath: String): Unit
}
