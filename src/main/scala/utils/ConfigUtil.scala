package edu.uic.llmforge
package utils

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtil {
  // Load default configuration from application.conf
  private val config: Config = ConfigFactory.load()

  // Define an immutable case class for only the required configuration fields
  case class AppConfig(
                        appName: String,
                        inputDatasetPath: String,
                        shardsDirectory: String,
                        encodingDirectory: String,
                        tokenOutputPath: String,
                        embeddingOutputPath: String,
                        embeddingCsvPath: String
                      )

  // Load default values from application.conf
  private val defaultConfig: AppConfig = AppConfig(
    appName = config.getString("app.name"),
    inputDatasetPath = config.getString("app.inputDatasetPath"),
    shardsDirectory = config.getString("app.shardsDirectory"),
    encodingDirectory = config.getString("app.encodingDirectory"),
    tokenOutputPath = config.getString("app.tokenOutputPath"),
    embeddingOutputPath = config.getString("app.embeddingOutputPath"),
    embeddingCsvPath = config.getString("app.embeddingCsvPath")
  )

  // Define an Option to hold the final configuration (immutable after being set)
  private var _finalConfig: Option[AppConfig] = None

  // Function to set the final configuration (from args or defaults)
  def initializeConfig(args: List[String]): Unit = {
    if (_finalConfig.isEmpty) {
      _finalConfig = Option(defaultConfig.copy(
        appName = args.headOption.getOrElse(defaultConfig.appName),
        inputDatasetPath = args.lift(1).getOrElse(defaultConfig.inputDatasetPath),
        shardsDirectory = args.lift(2).getOrElse(defaultConfig.shardsDirectory),
        encodingDirectory = args.lift(3).getOrElse(defaultConfig.encodingDirectory),
        tokenOutputPath = args.lift(4).getOrElse(defaultConfig.tokenOutputPath),
        embeddingOutputPath = args.lift(5).getOrElse(defaultConfig.embeddingOutputPath),
        embeddingCsvPath = args.lift(6).getOrElse(defaultConfig.embeddingCsvPath)
      ))
    }
  }

  // Access the final configuration anywhere in the program
  def finalConfig: AppConfig = _finalConfig.getOrElse(defaultConfig)
}
