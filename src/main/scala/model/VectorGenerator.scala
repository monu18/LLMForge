package edu.uic.llmforge
package model

import services.Encoder
import utils.CustomReshapeUtil

import org.deeplearning4j.nn.conf.layers.{EmbeddingSequenceLayer, OutputLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.slf4j.LoggerFactory

import java.io.PrintWriter

object VectorGenerator {

  val encoder = new Encoder
  private val logger = LoggerFactory.getLogger(getClass)

  // Create sliding window samples from a sequence of tokens
  def createSlidingWindowSamples(tokens: Seq[Int], windowSize: Int, stride: Int): Seq[(Array[Int], Int)] = {
    logger.info("Creating sliding window samples")
    tokens.sliding(windowSize + 1, stride).map { window =>
      val inputSeq = window.take(windowSize).toArray
      val targetToken = window.last
      (inputSeq, targetToken)
    }.toSeq
  }

  def trainAndSaveEmbeddings(decodedTokens: Seq[Int], windowSize: Int, stride: Int, outputFileName: String): (INDArray, Map[Int, Int]) = {
    logger.info("Starting training process")
    val (remappedDecoded, tokenToIndex) = remapTokens(decodedTokens)
    logger.info("Tokens remapped successfully")

    val inputOutputPairs = createSlidingWindowSamples(remappedDecoded, windowSize, stride)
    val (inputFeatures, outputLabels) = convertToIndArrays(inputOutputPairs)

    val vocabSize = tokenToIndex.size
    val embeddingDim = 50

    logger.info(s"Vocabulary size: $vocabSize, Embedding dimension: $embeddingDim")

    val config: MultiLayerConfiguration = new NeuralNetConfiguration.Builder()
      .weightInit(WeightInit.XAVIER)
      .list()
      .layer(0, new EmbeddingSequenceLayer.Builder().nIn(vocabSize).nOut(embeddingDim).build())
      .inputPreProcessor(1, new CustomReshapeUtil(windowSize, embeddingDim))
      .layer(1, new OutputLayer.Builder(LossFunction.SPARSE_MCXENT).nIn(embeddingDim * windowSize).nOut(vocabSize).activation(Activation.SOFTMAX).build())
      .build()

    val model = new MultiLayerNetwork(config)
    model.init()
    model.setListeners(new ScoreIterationListener(100))

    val numEpochs = 100
    for (epoch <- 1 to numEpochs) {
      logger.info(s"Starting epoch $epoch")
      model.fit(inputFeatures, outputLabels)
      logger.info(s"Completed epoch $epoch")
    }

    val embeddings: INDArray = model.getLayer(0).getParam("W")
    saveEmbeddings(outputFileName, embeddings, tokenToIndex)
    logger.info(s"Embeddings saved to $outputFileName")
    (embeddings, tokenToIndex)
  }

  private def remapTokens(decodedTokens: Seq[Int]): (Seq[Int], Map[Int, Int]) = {
    logger.info("Remapping tokens")
    val uniqueTokens = decodedTokens.distinct.sorted
    val tokenToIndex = uniqueTokens.zipWithIndex.toMap
    val remappedTokens = decodedTokens.map(tokenToIndex)
    (remappedTokens, tokenToIndex)
  }

  private def saveEmbeddings(filename: String, embeddings: INDArray, tokenToIndex: Map[Int, Int]): Unit = {
    logger.info(s"Saving embeddings to $filename")
    val pw = new PrintWriter(filename)
    try {
      val indexToToken = tokenToIndex.map(_.swap)
      val indexToWord = indexToToken.map { case (index, originalToken) =>
        val word = encoder.decode(Seq(originalToken))
        (index, word)
      }
      val rows = embeddings.rows()
      for (i <- 0 until rows) {
        val originalToken = indexToToken.getOrElse(i, -1)
        val word = indexToWord.getOrElse(i, "Unknown")
        val vector = embeddings.getRow(i).toDoubleVector.mkString(",")
        pw.println(s"$i,$originalToken,$word,$vector")
      }
      logger.info(s"Successfully saved $rows embeddings")
    } finally {
      pw.close()
      logger.info("PrintWriter closed")
    }
  }

  private def convertToIndArrays(inputOutputPairs: Seq[(Array[Int], Int)]): (INDArray, INDArray) = {
    logger.info("Converting input-output pairs to INDArray")

    // Extract input sequences
    val inputSequences: Array[Array[Int]] = inputOutputPairs.map { case (inputArray, _) =>
      inputArray
    }.toArray

    // Convert target tokens to Array[Int]
    val targetTokens: Array[Int] = inputOutputPairs.map { case (_, target) =>
      target
    }.toArray

    // Create INDArrays from the input sequences
    val inputFeatures: INDArray = Nd4j.create(inputSequences)
    logger.info(s"Input features shape: ${inputFeatures.shape.mkString(", ")}")

    // Create output labels INDArray from target tokens
    val outputLabels: INDArray = Nd4j.create(targetTokens.map(_.toDouble)).reshape(-1, 1)
    logger.info(s"Output labels shape: ${outputLabels.shape.mkString(", ")}")

    (inputFeatures, outputLabels)
  }
}
