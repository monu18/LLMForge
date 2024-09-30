package edu.uic.llmforge
package services

object TokenizerFactory {
  def createTokenizer(tokenizerType: String): Tokenizer = {
    tokenizerType match {
      case "encoder" => new Encoder()
      case "decoder" => new Decoder()
      case _ => throw new IllegalArgumentException(s"Unknown tokenizer type: $tokenizerType")
    }
  }
}
