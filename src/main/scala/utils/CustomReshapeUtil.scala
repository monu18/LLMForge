package edu.uic.llmforge
package utils

import org.deeplearning4j.nn.api.MaskState
import org.deeplearning4j.nn.conf.InputPreProcessor
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr
import org.nd4j.common.primitives.Pair
import org.nd4j.linalg.api.ndarray.INDArray

class CustomReshapeUtil(windowSize: Int, embeddingDim: Int) extends InputPreProcessor with Serializable {
  override def preProcess(input: INDArray, miniBatchSize: Int, workspaceMgr: LayerWorkspaceMgr): INDArray = {
    // Input shape: [batchSize, embeddingDim, windowSize]
    // Transpose to [batchSize, windowSize, embeddingDim]
    val transposed = input.permute(0, 2, 1)
    // Reshape to [batchSize, windowSize * embeddingDim]
    transposed.reshape('c', input.size(0), windowSize * embeddingDim)
  }

  override def backprop(output: INDArray, miniBatchSize: Int, workspaceMgr: LayerWorkspaceMgr): INDArray = {
    // Reshape to [batchSize, windowSize, embeddingDim]
    val reshaped = output.reshape('c', output.size(0), windowSize, embeddingDim)
    // Transpose back to [batchSize, embeddingDim, windowSize]
    reshaped.permute(0, 2, 1)
  }

  override def clone(): CustomReshapeUtil = new CustomReshapeUtil(windowSize, embeddingDim)

  override def getOutputType(inputType: InputType): InputType = ???

  override def feedForwardMaskArray(maskArray: INDArray, currentMaskState: MaskState, minibatchSize: Int): Pair[INDArray, MaskState] = ???
}
