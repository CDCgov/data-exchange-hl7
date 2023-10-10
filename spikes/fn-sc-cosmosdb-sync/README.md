### COSMOS DB SYNC FUNCTION

## Overview
This Function serves as a higher-level interface for processing and manipulating data before and after interactions with Azure CosmosDB. Utilizing the capabilities of the CosmosDBClient, the function streamlines operations, adding an extra layer of functionality on top of basic CRUD operations.

## Features

- The CosmosDBClient class offers a straightforward and intuitive interface for performing CRUD (Create, Read, Update, Delete) operations on Azure CosmosDB.
- It exposes five triggered functions:
  1. upsert-eventhub: EventHub trigger that supports batch processing.
  2. upsert-record: An HTTP-triggered REST API that upserts a record in the CosmosDB. If the record already exists, it's updated; otherwise, it's created.
  3. delete-record: An HTTP-triggered REST API that deletes a record using the provided record ID and partition key.
  4. update-timestamp: An HTTP-triggered REST API that updates the metadata/provenance/event_timestamp value in the event message for a specific record.
  5. reset: An HTTP-triggered REST API that resets record count, error count, and total runtime.
- The function is optimized using a Singleton factory design pattern to create Cosmos connections. This reduces the time spent rebuilding the client and connecting to the database and connector.
- Provides the record count and runtime duration (in milliseconds) for each function call, and the total for batch processing on the EventHub. For HTTP REST calls, the runtime duration can be found in the response header.

## Usage on localhost

- upsert-eventhub: [eventHubTrigger]
- upsert-record: [POST] http://localhost:7071/cosmossync/upsert-record
  - Include the JSON message in the body of the request.
- delete-record: [GET,POST] http://localhost:7071/cosmossync/delete-record
  - headers
    - id: The ID of the record you wish to delete.
    - partition-key: The partition key for the record.
- update-timestamp: [GET,POST] http://localhost:7071/cosmossync/update-timestamp
  - headers
    - id: The ID of the record you wish to update.
    - partition-key: The partition key for the record.
- reset: [GET,POST] http://localhost:7071/cosmossync/reset

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

add this to host.json root

"extensions": {<br>
&ensp;"http": {<br>
&ensp;&ensp;"routePrefix": "cosmossync"<br>
&ensp;}<br>
}
