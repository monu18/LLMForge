package edu.uic.llmforge
package utils

object SimilarityUtil {
  def cosineSimilarity(vec1: Array[Double], vec2: Array[Double]): Double = {
    val dotProduct = vec1.zip(vec2).map { case (a, b) => a * b }.sum
    val magnitude1 = math.sqrt(vec1.map(x => x * x).sum)
    val magnitude2 = math.sqrt(vec2.map(x => x * x).sum)
    if (magnitude1 == 0.0 || magnitude2 == 0.0) 0.0
    else dotProduct / (magnitude1 * magnitude2)
  }

}
