
# Receiver-Debatcher Function for the HL7 Pipeline

# TL;DR>

This service receives HL7 data uploaded to DEX and makes it available to be processed via our HL7 pipeline.
	
	
# Details:
The Receiver Debatcher service listens to BlobCreate events on a given container in AZ blob storage and process those files.

Files dropped here can be single HL7 files or batches of HL7 files. Batches might optionally contain HL7 Batch headers (FHS, BHS) and Footer (FTS, BTS) segments.

An AZ Container is setup with Events to generate a new Event on BlobCreate to push messages to an event hub topic. (<code>hl7-file-dropped</code>). This service will be a consumer of such topic and further process the event.

This service, upon receiving the event, will read the actual data from the blob storage and determine if the data is a single HL7 message or batch of messages. In either case, each message will be enriched with provenance metadata (file-name, size, creation timestamp, single or batch,  message index, event id and timestamp), user submitted metadata (external system provider, original file name and timestamp) and some generated Ids to track the message along the pipeline (file_uuid and message_uuid). 

Both the generated metadata described above and the actual HL7 content (base-64 encoded) will be propagated to an event hub (hl7-recdeb-ok).

If the message does not appear to be an HL7 message, it will be propagated to a dead-letter event hub topic (hl7-recdeb-err).

![image](https://user-images.githubusercontent.com/3239945/205654635-4645456f-f706-48ff-9ced-49443407045a.png)


## Pipelne:

* **Input**: hl7-file-dropped
* **Output**: hl7-recdeb-ok ; hl7-recdeb-err

## Hl7-recedeb-ok payload:

``` json
{
 "content": "Base64(MSH|^~\&|....)",
 "meta_message_uuid": "",
 "summary": {
 "current_status": "RECEIVED"
 },
 "metadata": {
   "provenance": {
     "event_id": "1234-56789",
     "event_timestamp": "2022-10-12:59:00.000",
     "file_path": "abfss://container@storage/folder/filename.txt",
     "file_timestamp": "2022-10-12:58:00.000",
     "file_size": 1024,
     "file_uuid": "1234-56789",
     "single_or_batch": "SINGLE|BATCH",
     "message_index": 1,
     "message_hash": "asdf34-nweoru734",
     "ext_system_provider": "MVPS",
     "ext_original_file_name": "abc.txt",
     "ext_original_file_timestamp": ""
   },
   "processes": [
     {
     "process_name": "Receiver",
     "start_processing_time": "2022-10-01T13:00:00.000",
     "end_processing_time": "2022-10-01T13:01:00.000",
     "process_version": "1.0.0",
     "status": "SUCCESS"
     }
   ]
 },
 "metadata_version": "1"
}
``` 

## update 2022-112-01
It was decided that for routing purposes, this service will create more metadata around which program owns this message.

### Message_info payload:

```json
"message_info": {
     "event_code": "11088",
     "profile: "Lyme_TBRD_MMG_V1.0", 
     "mmgs": [ "GenV2", "TBRD"]
     "reporting_jurisdiction": 13
     "patient_id": "xyz" 
}


```
![Uploading image.png…]()


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
