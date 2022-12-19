
## MMG-Validator Function for the HL7 Pipeline

# TL;DR>

This service can validate a hl7 message content-wise using MMG  profiles created in [MMG-AT]()tool.

# Details

The MMG Validator service validates messages against their specific MMG. Currently, the service supports the following MMGs:

* GenV1 Case 
* GenV1 Summary
* GenV2
* Lyme/TBRD
* Hepatitis
* Arboviral 1.0, 1.2, 1.3

The following rules are checked against each HL7 message:

* **Cardinality** - validate the number of answers on the message matches the cardinality specified o the MMG for that specific field.
* **Data Types** - validates that the Data types on OBX-2 matches the data type defined by the MMG
* **Vocabulary** - for CE and CWE fields, the codes provided on OBX-5 are checked against Value Sets from PhinVADS.
* **Invalid OBX segments** - if an OBX-3 code is present on the message that is not mapped on the MMG will be flagged as a warning.



![image](https://user-images.githubusercontent.com/3239945/208454032-b4169ed4-1a48-41c5-a603-20f8fbf6631e.png)


## Pipelne:

Input: hl7-struct-ok

Output: hl7-mmg-valid-ok ; hl7-mmg-valid-err

## Hl7-mmg-ok payload:

``` json
 "content": "Base64(MSH|^~\&|....)",
 "meta_message_uuid": "",
 "summary": {
    "current_status": "STRUCTURE_VALID"
 },
 "metadata": {
    "provenance": {
	{unchanged}
       },
     "processes": [
	 {
		 "process_name": "Receiver",
		 "start_processing_time": "2022-10-01T13:00:00.000",
		 "end_processing_time": "2022-10-01T13:01:00.000",
		 "process_version": "1.0.0",
		 "status": "SUCCESS"
	 },
	 {
		 "process_name": "Structure-Validator",
		 "start_processing_time": "2022-10-01T13:02:00.000",
		 "end_processing_time": "2022-10-01T13:03:00.000",
		 "process_version": "1.0.0",
		 "status": "SUCCESS"
		"report": {
		  {full NIST Report}
		}
	 },
	 {
		 "process_name": "MMG-Validator",
		 "start_processing_time": "2022-10-01T13:03:00.000",
		 "end_processing_time": "2022-10-01T13:04:00.000",
		 "process_version": "1.0.0",
		 "status": "SUCCESS"
		"report": {
		  {full MMG Validation Report}
		}
	 }
   ]
 },
 "metadata_version": "1"
}

```


