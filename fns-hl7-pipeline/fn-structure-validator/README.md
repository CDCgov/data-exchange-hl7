## Structure-Validator Function for the HL7 Pipeline

# TL;DR>

This service validates an HL7 message structurally using PHIN Spec profiles created in the IGAMT tool.

# Details

The Structure Validator listens to an event topic that should contain single messages (already debatched and redacted) and uses the [lib-nist-validator](https://github.com/CDCgov/data-exchange-hl7/blob/develop/local_libs/lib-nist-validator/readme.md) library along with profiles created using the [IGAMT](https://hl7v2.igamt-2.nist.gov/home) tool.

Currently, the Structure Validator can validate any PHIN Spec version (2.0, 3.0, 3.1 or 3.2) for CASE notifications and HL7 2.3.x and 2.5.1 Implementation Guides for COVID-19 ELR messages.
For CASE, the **MSH-21[1].1** field specifies the PHIN Spec version the message abides by, and that field is used to load the appropriate specification for validation.
For ELR, the service is dependent on metadata submitted by the uploader. The Receiver/Debatcher will read Azure object metadata and make that available in our event metadata under message_info. The Structure Validator will use **message_info.route** as the prefix of the profiles to be used plus **MSH-12** (version) to fully know what profiles to load. **COVID19_ELR** is the only route currently supported as of today (2023-05), and we support versions 2.3, 2.3.1, 2.3.Z, 2.5 and 2.5.1.

As a first step, the Structure Validator makes sure that the message starts with "MSH" followed by a pipe delimiter ("**|**") and the exact encoding characters **"^~\\&"**.
In other words, the message must start with 

```
"MSH|^~\\&"
```

All IGAMT profiles are stored within this project under [/src/main/resources](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-structure-validator/src/main/resources) folder, with one folder for each set of files that belong to a specific profile:

(Note: This was feasible when we thought we would have only the 4 case profiles. If the number of profiles keeps growing, perhaps this decision needs to be revisited for a more robust solution).

* For Case:
	* NND_ORU_V2.0 - for PHIN Spec 2.0
	* NOTF_ORU_V3.0 - for PHIN Spec 3.0
	* NOTF_ORU_V3.1 - for PHIN Spec 3.1* ( [Current Specification](https://ndc.services.cdc.gov/wp-content/uploads/PHIN_Messaging_Specification_for_Case_Notification_v3-1-1_20210805.docx) )
	* NOTF_ORU_V3.2 - for PHIN Spec 3.2* (Future specification coming soon)
* For COVID-19 ELR:
	* COVID19_ELR-v2.3 
	* COVID19_ELR-v2.3.1
	* COVID19_ELR-v2.3.Z
	* COVID19_ELR-v2.5
	* COVID19_ELR-v2.5.1


The service will receive a single message with all metadata already enriched on this message by the previous functions (Receiver/Debatcher and Redactor). The Structure Validator will then read the message, identify the profile to be used for structure validation, perform the validation using the lib-nist-validator library and append the relevant metadata to the event payload along with the entire validation report.

The **metadata.summary** element is also updated to reflect the current status of the message, indicating whether the structure validation succeeded.

This service splits CASE messages and ELR messages into two separate Event Hubs. ELR messages do not go through MMG validation, and therefore they are separated from the CASE messages topic where MMG-validation service is listening for messages.

### Notes on application.conf.

The NIST validator libraries use an <code>application.conf</code> file to somewhat control how issues are flagged. A specific issue/problem can be flagged as Error or Warning (among others not used by DEX).
We made some alterations to this file in order to better control DEX validation:

- Any Length issue is flagged as a Warning.
- Any X-usage issue is flagged as a Warning.
- Any cardinality and null-cardinality issue is flagged as a Warning.


## Diagram 

![image](https://user-images.githubusercontent.com/3239945/233380847-1bef3d9b-21dd-414f-a856-78a59c5ee44f.png)

## Pipeline:

 - Input: hl7-redacted-ok 
 - Outputs: 
    - hl7-structure-ok with Valid CASE messages
    - hl7-structure-elr-ok with Valid ELR messages
    - hl7-structure-err with all Invalid Messages

## hl7-structure-ok payload:

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
		 "process_name": "Redactor",
		 "start_processing_time": "2022-10-01T13:01:00.000",
		 "end_processing_time": "2022-10-01T13:02:00.000",
		 "process_version": "1.0.0",
		 "status": "SUCCESS"
	 },	 
	 {
		 "process_name": "Structure-Validator",
		 "start_processing_time": "2022-10-01T13:02:00.000",
		 "end_processing_time": "2022-10-01T13:03:00.000",
		 "process_version": "1.0.0",
		 "status": "SUCCESS",
		 "configurations": [
		 	"NOTF_ORU_v3.0"
		 ],
		 "eventhub_queued_time": "2022-10-01T13:03:00.000",
		 "eventhub_offset": 12345678890,
		 "eventhub_sequence_number": 123,
		 "report": {
		  {full NIST Report}
		}
	 }
	
   ]
 },
 "metadata_version": "1"
}

```


