
# Receiver-Debatcher Function for the HL7 Pipeline

# TL;DR>

This service receives HL7 data uploaded to DEX and makes it available to be processed via our HL7 pipeline. It currently supports Case Notification messages and ELR.
	
	
# Details:
The Receiver Debatcher service listens to BlobCreate events on a given container in AZ blob storage and processes those files.

Files dropped here can be single HL7 files or batches of HL7 files. Batches might optionally contain HL7 Batch header (FHS, BHS) and Footer (FTS, BTS) segments.

An AZ Container is setup with Events to generate a new Event on BlobCreate to push messages to an event hub topic. (<code>hl7-file-dropped</code>). This service will be a consumer of such a topic and further process the event.

Each file must be uploaded with certain metadata attached to that object, as follows:
- **message_type**: [Required] Indicates whether the message being uploaded is a "CASE" message or an "ELR" message. (Only those two values are suported so far.)
- **reporting_jurisdiction**: [Required if message_type == "ELR"] Indicates the Jurisdiction submitting the message. ELR does not contain information about reporting jurisdiction within the message, and therefore it must be provided as metadata.
- **route**: [Required if message_type == "ELR"] Indicates the program owning this message and to where should it be routed. Currently, only the value "COVID19_ELR"  is supported for route.
- **system_provider**: [Optional] Indicates which system is performing the upload. Ex.: DEX_Upload, PHINMS, Mercury, etc.
- **orginal_file_name**: [Optional] Uploader can identify the original file name from the system of origin.
- **original_file_timestamp**: [Optional] Uploader can identify the original file timestamp from the system of origin.

This service, upon receiving the event, will read the actual data from the blob storage and determine whether the data is a single HL7 message or a batch of messages. In either case, each message will be enriched with provenance metadata (filename, size, creation timestamp, single or batch,  message index, event id and timestamp), user submitted metadata (external system provider, original file name and timestamp) and some generated IDs to track the message along the pipeline (file_uuid and message_uuid). 
Basic metadata validation will be performed for the required attributes above. If validation fails, the message will be dead-lettered.

Both the generated metadata described above and the actual HL7 content (base-64 encoded) will be propagated to an event hub (hl7-recdeb-ok).

If the message does not appear to be an HL7 message, it will be sent to a dead-letter event hub topic (hl7-recdeb-err).

![image](https://user-images.githubusercontent.com/3239945/205654635-4645456f-f706-48ff-9ced-49443407045a.png)


## Pipelne:

* **Input**: hl7-file-dropped
* **Output**: hl7-recdeb-ok ; hl7-recdeb-err

## Hl7-recedeb-ok payload:

``` json
{
 "content": "Base64(MSH|^~\&|....)",
 "message_uuid": "",
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
     "ext_system_provider": "DEX_UPLOAD",
     "ext_original_file_name": "abc.txt",
     "ext_original_file_timestamp": ""
   },
   "processes": [
     {
     "process_name": "Receiver",
     "start_processing_time": "2022-10-01T13:00:00.000",
     "end_processing_time": "2022-10-01T13:01:00.000",
     "process_version": "1.0.0",
     "eventhub_queued_time": "2022-10-01T13:02:00.000",
     "eventhub_offset": 1234567890,
     "eventhub_sequence_number": 123
     "status": "SUCCESS"
     }
   ]
 },
 "metadata_version": "1"
}
``` 

## update 2022-11-01
It was decided that for routing purposes, this service will create more metadata around which program owns this message.

### Message_info payload:

```json
"message_info": {
     "event_code": "11088",
     "profile: "Lyme_TBRD_MMG_V1.0", 
     "mmgs": [ "GenV2", "TBRD"],
     "reporting_jurisdiction": 13
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
