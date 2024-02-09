# HL7V2 Pipeline Test Harness

## Overview

The HL7V2 Pipeline Test Harness exists to:

- **Facilitate End-to-End Testing:** Streamline the process of testing HL7v2 pipeline from message ingestion to payload verification, ensuring comprehensive coverage.

- **Automate Key Processes:** Automate critical tasks such as dropping messages to Blob Storage, running them through the pipeline, and validating the payloads in Cosmos DB.

- **Ensure Reliability and Accuracy:** By providing a structured testing workflow, the test harness aims to enhance the reliability and accuracy of HL7v2 message processing.

- **Support Multiple Message Types:** While currently supporting PHLIP_FLU-2.5.1 messages, the tool is designed with extensibility in mind, with plans to support additional message types in the future.


### How It Works

- **Payload Analysis:**
    - This is a manual step where payloads are analyzed against the Specifications file and profiles created by the IGAMT tool.

- **Resource Storage For Test HL7V2 Messages:**
    - The messages are stored in main/resources/messages folder.

- **Blob Storage Interaction:**
    - Using BlobServiceClientBuilder() library, messages are uploaded in the designated blob container.

- **Metadata Attachment:**
    - Metadata object is attached to each uploaded blob.

- **Pipeline Execution:**
    - receiver-debatcher function in the HL7v2 is triggered and the messages are traversed through the pipeline.

- **Cosmos DB Querying:**
    - Utilizing Cosmos Client library from lib-dex-commons to query the specified container for each message, fetch the payload and  store it in the 'test/resources/new-payloads' folder.

- **Payload Verification:**
    - Create Tests with Junit and compare results against pre-verified payloads.

![Test Harness Diagram](https://github.com/CDCgov/data-exchange-hl7/assets/137535421/8a31010d-3390-4d7f-9c0e-af228b4589ba)



## Required Configuration Parameters
- **BLOB_CONTAINER_NAME**: Name of the blob container where messages will be dropped.
- **BLOB_INGEST_CONNECTION_STRING**: The Blob storage connection string.
- **COSMOS_DB_KEY**: Access key of the Cosmos DB instance.
- **COSMOS_DB_ENDPOINT**: The URL endpoint of your Cosmos DB instance.
- **COSMOS_DB_PARTITION_KEY**: The path of the partition key.
- **RECEIVER_DEBATCHER**: The name of the receiver-debatcher container in Cosmos DB.
- **REDACTOR**: The name of the redactor container in Cosmos DB.
- **STRUCTURE_VALIDATOR**: The name of the structure validator container in Cosmos DB.
- **JSON_LAKE**: The name of the json lake transformer container in Cosmos DB.
- **LAKE_OF_SEGMENTS**: The name of the lake of segments transformer container in Cosmos DB.


## Usage
TBD

