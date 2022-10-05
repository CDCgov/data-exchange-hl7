
# Receiver-Debatcher Function for the HL7 Pipeline

## Development setup.

1. Clone this repo on your local machine.
1. Create a <code>local.settings.json</code> file under <code>fn-receiver-debatcher/function</code> folder. 
1. Define the following properties on the file above:
```json
{
  "IsEncrypted": false,
  "Values": {
    "EventHubSendOkName": "<event hub to use for Ok messages>",
    "EventHubSendErrName": "<event hub to use for error message>",
    "EventHubConsumerGroup": "<event hub consumer group name>",
    "EventHubConnectionString": "<event hub connection string.>",
    "BlobIngestContainerName": "<container name>",
    "BlobIngestConnectionString": "<Storage Acct. connection string>"
  },

  "Host": {
    "LocalHttpPort": 7073
  }
}
```

1. run <code>mvn package</code>
1. run <code>mvn azure-functions:run</code> to execute the function locally.

### Function project was started with:

[microsoft kotlin-maven](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-first-kotlin-maven?tabs=bash )

```
mvn archetype:generate \
    -DarchetypeGroupId=com.microsoft.azure \
    -DarchetypeArtifactId=azure-functions-kotlin-archetype
```
