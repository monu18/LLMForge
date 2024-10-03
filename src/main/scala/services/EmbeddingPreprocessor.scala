package edu.uic.llmforge
package services

import edu.uic.llmforge.utils.CustomReshapeUtil
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.api.buffer.DataType
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration}
import org.deeplearning4j.nn.conf.layers.{EmbeddingSequenceLayer, OutputLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.deeplearning4j.optimize.listeners.ScoreIterationListener

import java.io.{File, FileWriter, PrintWriter}

object EmbeddingPreprocessor {
  val encoder = new Encoder

  def generateEmbeddingsForTokens(encodedTokens: Seq[Int], windowSize: Int, stride: Int): Map[Int, INDArray] = {
    val (remappedDecoded, tokenToIndex) = remapTokens(encodedTokens)
    val inputOutputPairs = encoder.createInputOutputPairs(remappedDecoded, windowSize, stride)
    val (inputFeatures, outputLabels) = convertToIndArrays(inputOutputPairs)

    val vocabSize = tokenToIndex.size
    val embeddingDim = 50

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
      model.fit(inputFeatures, outputLabels)
      println(s"Completed epoch $epoch")
    }

    val embeddings: INDArray = model.getLayer(0).getParam("W")
    val indexToToken = tokenToIndex.map(_.swap) // Reverse the map to get index -> token
    val embeddingsMap: Map[Int, INDArray] = (0 until embeddings.rows()).map { rowIndex =>
      val tokenId = indexToToken(rowIndex)
      tokenId -> embeddings.getRow(rowIndex).dup() // Make sure to duplicate the row to avoid issues with shared memory
    }.toMap

    // Return the map containing each token ID mapped to its embedding vector
    embeddingsMap
  }

  def remapTokens(decodedTokens: Seq[Int]): (Seq[Int], Map[Int, Int]) = {
    val uniqueTokens = decodedTokens.distinct.sorted
    val tokenToIndex = uniqueTokens.zipWithIndex.toMap
    val remappedTokens = decodedTokens.map(tokenToIndex)
    (remappedTokens, tokenToIndex)
  }

  def saveEmbeddingToCSV(tokenID: Int, tokenWord: String, embeddingStr: String): Unit = {
    val csvFilePath = "src/main/resources/output/embeddings.csv"
    val file = new File(csvFilePath)
    val append = file.exists()

    // Create the parent directory if it doesn't exist
    val parentDir = file.getParentFile
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs()
    }

    val pw = new PrintWriter(new FileWriter(file, append)) // Open in append mode if file exists
    try {
      // Write the header if it's a new file
      if (!append) {
        pw.println("TokenID,Word,Embeddings")
      }
      pw.println(s"$tokenID,$tokenWord,$embeddingStr")
    } finally {
      pw.close()
    }
  }

  def convertToIndArrays(inputOutputPairs: Seq[(Array[Int], Int)]): (INDArray, INDArray) = {
    val inputSequences: Array[Array[Double]] = inputOutputPairs.map { case (inputArray, _) =>
      inputArray.map(_.toDouble)
    }.toArray

    // Convert target tokens to Array[Double]
    val targetTokens: Array[Double] = inputOutputPairs.map { case (_, target) =>
      target.toDouble
    }.toArray

    // Create INDArrays from the arrays
    val inputFeatures: INDArray = Nd4j.create(inputSequences).castTo(DataType.INT32)
    val outputLabels: INDArray = Nd4j.create(targetTokens).reshape(-1, 1).castTo(DataType.INT32)

    (inputFeatures, outputLabels)

  }

}
