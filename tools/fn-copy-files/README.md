# Azure Function: Cosmos DB Container Copy

This Azure Function provides an HTTP-triggered endpoint for copying all documents from one Cosmos DB container to another, potentially across different databases or even different Cosmos DB accounts.

## Features

- HTTP-triggered function with dynamic routing
- Supports different source and destination endpoints and keys
- Optional custom partition key support
- Validates the presence of necessary headers before proceeding with the copy operation

## Prerequisites

Before you begin, ensure you have:

- An Azure subscription
- Two Cosmos DB accounts (if copying across accounts) with the source and destination containers created
- Appropriate permissions to read from the source and write to the destination containers

## Usage

The function is triggered via HTTP request to the endpoint with the following route pattern:

`/copy/{fromDb}/{fromContainer}/{toDb}/{toContainer}`


### Headers

- `source-endpoint` (required): The Cosmos DB URI endpoint for the source container.
- `source-key` (required): The primary or secondary key for the source Cosmos DB account.
- `destination-endpoint` (optional): The Cosmos DB URI endpoint for the destination container. If not provided, `source-endpoint` will be used.
- `destination-key` (optional): The primary or secondary key for the destination Cosmos DB account. If not provided, `source-key` will be used.
- `partition-key` (required): The partition key path used in both the source and destination containers. If not provided, defaults to the partition key path configured in the function.

### Responses

- `200 OK`: The operation was successful, and all documents were copied.
- `400 Bad Request`: Necessary information was missing or incorrect in the request headers.
- `500 Internal Server Error`: An error occurred during the copy operation. Details are provided in the response body.