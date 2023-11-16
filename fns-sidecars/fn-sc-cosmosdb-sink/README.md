# COSMOS DB SINK SIDECAR FUNCTION

## Overview
This Function upserts the incoming records from the Event Hub.  This function connects to Cosmos DB SDK through lib-dex-commons CosmosClent class.

## Configuration

Set the following environment variables for your Azure Function:

- `CosmosEndpoint`: The endpoint URI of the Cosmos DB account.
- `CosmosKey`: The access key for your Cosmos DB account.
- `CosmosDBId`: The ID of your Cosmos DB database.
- `CosmosContainerId`: The name of your Cosmos DB container.
- `partitionKeyPath`: The partition key path for your Cosmos DB container.
- `EventHubReceiveName`: The name of the Event Hub.
- `EventHubConnectionString`: The connection string for the Event Hub.
- `EventHubConsumerGroup`: The name of the consumer group within the Event Hub.
