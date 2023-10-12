### COSMOS DB SYNC SIDECAR FUNCTION

## Overview
This Function syncronizes the data between an EventHub and Cosmos DB.  Utilizing the capabilities of the CosmosDBClient, the Function connects using Cosmos DB SDK.

## Features

- The CosmosDBClient class offers a straightforward and intuitive interface for performing the Create All operation on Azure CosmosDB.
- It exposes two triggered functions:
  1. cosmos-sync: EventHub trigger that supports batch processing.
  2. close-cosmos: An HTTP-triggered REST API that closes the Cosmos DB Client.
- The function is optimized using a Singleton factory design pattern to create Cosmos connections. This reduces the time spent rebuilding the client and connecting to the database and connector.
- The function batches all records found in the event hub
- Provides the record count and runtime duration (in milliseconds) for each function call, and the total for batch processing on the EventHub. For HTTP REST calls, the runtime duration can be found in the response header.

## Local setup
Provide the following values in local.setting.json

{<br>
&ensp;"Values": {<br>
&ensp;&ensp;"AzureWebJobsStorage": "DefaultEndpointsProtocol=https;AccountName=tfhl7appstorage;AccountKey=<redacted_here>;EndpointSuffix=core.windows.net",<br>
&ensp;&ensp;"EventHubConnectionString": "Endpoint=sb://tf-eventhub-namespace-dev.servicebus.windows.net/;SharedAccessKeyName=azfn-eh-namespace-key;SharedAccessKey=<redacted_here>",<br>
&ensp;&ensp;"EventHubReceiveName": "hl7-<fn_name>-ok",<br>
&ensp;&ensp;"EventHubConsumerGroup": "<consumer_group>",<br>
&ensp;&ensp;"CosmosConnectionString": "https://ocio-ede-dev-hl7-db-cosmos-account.documents.azure.com:443/",<br>
&ensp;&ensp;"CosmosKey": "<redacted_here>",<br>
&ensp;&ensp;"cosmosRegion": "East US",<br>
&ensp;&ensp;"CosmosDBId": "hl7-events",<br>
&ensp;&ensp;"CosmosContainer": "<container_name>"<br>
&ensp;}<br>
}
