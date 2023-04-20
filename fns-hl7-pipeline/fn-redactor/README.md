# TL;DR>
This service redacts PII data out of the original message. Redacted fields are configurable.
	
	
# Details:
This field uses HL7-PET library to perform redaction on specific fields based on configuration files. 
It loads different configuration files for CASE message vs ELR messages. It inspects specific fields (defined as HL7 paths, ex.: PID[1]-11.9) for the presence of any value and if populated, it will substitute the field out based on the Rule.
	
## PID-5
It was agreed that for PID-5, we don't want to fully "fix" the message.
CDC requires STLTs to send **PID-5** as <code>"~^^^^^^S"</code>

Redactor service expects **PID-5[2]** to be precisely <code>^^^^^^S</code>. And it will not "fix" a message if it was sent with any other value.

The way this was implemented is - first, the redactor checks for the presence of <code>^^^^^^S</code> in** PID-5[2]** and loads a specific configuration file depending on whether this field is properly set or not.
Then it proceeds to redact **PID-5[1]** and possibly **PID-5[2]** to remove any PII data present.

If **PID-5[2]** is not the appropriate value, Redactor will remove the PII data from the message and the Structure Validator will error out this message. 

Therefore it's important that STLTS send **PID-5[2]** as defined on the PHIN Spec with the exact value of "^^^^^^S.

Ex.:

|Original Message PID-5 Value|Redacted Message PID-5|Status|
|---|---|---|
|~^^^^^^S|~^^^^^^S|Valid Message|
|John Doe~^^^^^^S|~^^^^^^S|Valid Message|
|John Doe~Jonny|~|Invalid message|
|John Doe|<empty>|Invalid Message|
	
		
	
Diagram
  
![image](https://user-images.githubusercontent.com/3239945/233375787-ae64ac17-70c5-486e-9e1c-be0b27a09591.png)

	
	
	
## Pipelne:
  
- Input: hl7-recdeb-ok
- Outputs: hl7-redacted-ok ; hl7-redacted-err

## Hl7-redacted-ok payload:

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
	"eventhub_queued_time": "2022-10-01T13:02:00.000",
	"eventhub_offset": 1234567890,
	"eventhub_sequence_number": 123,
	 "status": "SUCCESS"
	 },
	 {
	 "process_name": "Redactor",
	 "start_processing_time": "2022-10-01T13:00:00.000",
	 "end_processing_time": "2022-10-01T13:01:00.000",
	 "process_version": "1.0.0",
	"eventhub_queued_time": "2022-10-01T13:02:00.000",
	"eventhub_offset": 222222222222222,
	"eventhub_sequence_number": 2223,
	"configurations": [
	    "case_config.txt"
	 ]
	
	 "status": "SUCCESS",
	 "report": { …}
	 }
	
   ]
 },
 "metadata_version": "1"
}

```

