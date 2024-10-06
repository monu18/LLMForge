# Distributed (LLM) Processing: Encoding, Embedding, and Semantic Analysis on Hadoop & AWS EMR​
# Author: Monu Kumar
# Email: mkuma47@uic.edu

## Introduction
This homework assignment for CS441 focuses on implementing an encoder for a Large Language Model (LLM) from scratch using distributed computing in the cloud. In this phase, we will create a mapper and reducer using Apache Hadoop to process a given text corpus, generating a YAML or CSV file containing token embeddings and relevant statistics. After testing the program locally, we will deploy and run it on Amazon Elastic MapReduce (EMR).

**Video Link:** [Video Link] (The video explains the deployment of the Hadoop application in the AWS EMR Cluster and the project structure.)

## Environment
- **OS:** Mac
- **IDE:** IntelliJ IDEA 2024.2.1 (Ultimate Edition)
- **Scala Version:** 3.5.0
- **SBT Version:** 1.10.3
- **Hadoop Version:** 3.3.6

## Running the Test File
Test files can be found under the directory `src/test`:
```bash
sbt clean compile test
```

## Running the Project
1. **Clone this repository:**
   ```bash
   git clone https://github.com/monu18/LLMForge
   ```
2. **Navigate to the Project:**
   ```bash
   cd LLMForge
   ```
3. **Checkout main branch(if not):**
   ```bash
   git checkout origin main
   ```   
4. **Open the project in IntelliJ:**  
   [How to Open a Project in IntelliJ](https://www.jetbrains.com/help/idea/import-project-or-module-wizard.html#open-project)

## Configuration Utility for LLMForge
The `ConfigUtil` object, located in `src/main/scala/utils/ConfigUtil.scala`, is responsible for managing application configurations for the LLMForge project. This utility loads default configurations from the `application.conf` file found in `src/main/resources/`, allowing for flexibility in path settings based on command-line arguments.

### Configuration Behavior:
- **Initialization:** The `initializeConfig` method accepts a list of command-line arguments. If provided, these arguments will override the corresponding default configuration values. If no arguments are given, the application defaults to the paths defined in `application.conf`.
- **Final Configuration Access:** The application can access the final configuration via the `finalConfig` method, ensuring that once set, the configuration remains immutable.

### Configuration Paths:
- **appName:** The name of the application (default: "LLMForge").
- **inputDatasetPath:** The path to the input dataset file (default: `src/main/resources/input/wikitext_train.txt`).
- **shardsDirectory:** The directory for storing output shards (default: `src/main/resources/output/shards`).
- **encodingDirectory:** The directory for saving encoding output files (default: `src/main/resources/output/encodingoutput`).
- **tokenOutputPath:** The path for writing tokenized output (default: `src/main/resources/output/tokens/tokens.txt`).
- **embeddingOutputPath:** The directory for saving embedding files (default: `src/main/resources/output/embeddings`).
- **embeddingCsvPath:** The path for the CSV file containing embeddings (default: `src/main/resources/output/embeddings.csv`).
- **semanticsOutputPath:** The directory where semantic analysis results are stored (default: `src/main/resources/output/semantics`).

## Project Structure
The project comprises the following key components:

1. **Data Cleaning**  
   The initial step involves cleaning the input text corpus to remove any irrelevant data or noise. This ensures that the data is in a usable format for subsequent processing.

2. **Sharding**  
   Shards are generated from the cleaned data to facilitate parallel processing. This involves dividing the text corpus into smaller, manageable segments, allowing for efficient distributed computation.

3. **MapReduce Jobs**  
   The project implements three main MapReduce jobs to encode and generate embeddings from the text data:
   - **Word Count Job:** Processes the input shards to count the occurrences of each word in the text corpus, generating a mapping of words to their frequencies.
   - **Embedding Vector Generator Job:** Utilizes Deeplearning4j to compute embedding vectors for each unique word based on the word counts generated from the previous step.
   - **Semantics Job:** Focuses on generating cosine similarity scores between the embedding vectors produced earlier, calculating semantic similarity based on their embeddings.

4. **Tokenization**  
   The project employs JTokkit as the tokenizer to convert the raw text into tokens, preparing the text data for the subsequent encoding and embedding processes.

5. **Cosine Similarity Generation**  
   The project uses cosine similarity to quantify the similarity between word vectors, enabling the identification of words with similar meanings based on their context.

## Prerequisites
Before starting the project, ensure you have the following tools and accounts set up:
- **Hadoop:** Install and configure Hadoop on your local machine or cluster.
- **AWS Account:** Create an AWS account and familiarize yourself with AWS Elastic MapReduce (EMR).
- **Java:** Ensure that Java is installed and properly configured.
- **Git and GitHub:** Use Git for version control and host your project repository on GitHub.
- **IDE:** Choose an Integrated Development Environment (IDE) for coding and development.

## Conclusion
This project aims to develop a Large Language Model (LLM) encoder through a systematic approach involving data cleaning, sharding, and MapReduce jobs for word counting, embedding generation, and semantic analysis. By utilizing JTokkit for tokenization and Deeplearning4j for embedding, we are leveraging powerful tools to create a robust LLM framework. The successful completion of this project will enhance our understanding of distributed computing and natural language processing while providing practical experience with cloud-based technologies.
