# TL;DR>

This service can validate a hl7 message structurally using PHIN Spec profiles created in IGAMT tool.

# Details

The structure validator listens to an event topic that should contain single messages (already debatched) and uses the [lib-nist-validator](https://github.com/CDCgov/data-exchange-hl7/blob/develop/local_libs/lib-nist-validator/readme.md) library along with profiles created using the [IGAMT](https://hl7v2.igamt-2.nist.gov/home) tool.

Currently, structure validator can validate any PHIN Spec version - 2.0, 3.0, 3.1 or 3.2.
The MSH-21[1].1 field specifies the Phin Spec version a specific message abides by and that field is used to load the appropriate specification before validation.

All IGAMT profiles are stored within this project under [/src/main/resources](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-structure-validator/src/main/resources) folder, with one folder for each set of files that belong to a specific profile:

* NND_ORU_V2.0 - for Phin Spec 2.0
* NOTF_ORU_V3.0 - for Phin Spec 3.0
* NOTF_ORU_V3.1 - for Phin Spec 3.1* ( [Current Specification](https://ndc.services.cdc.gov/wp-content/uploads/PHIN_Messaging_Specification_for_Case_Notification_v3-1-1_20210805.docx) )
* NOTF_ORU_V3.2 - for Phin Spec 3.2* (Future specification coming soon)

The service will receive a single message with all metadata already enriched on this message by the Receiver-Debatcher, read the message, identify the profile to be used for structure validation, perform the validation using the lib-nist-validator library and append the relevant metadata to the event payload along with the entire validation report.

==> **TODO::Update paragraph above when Redactor service is in place**

The metadata.summary element will also be updated to reflect the current status of the message and indicates whether the structure validation succeeded and a quick status of the validation itself.

![image](https://user-images.githubusercontent.com/3239945/206724600-735edf11-80a5-4a5e-8287-d3ebf95a3ea3.png)

## Pipelne:

Input: hl7-recdeb-ok (eventually it will be hl7-redacted-ok when Redactor service is in place)

Output: hl7-structure-ok ; hl7-structure-err

## Hl7-structure-ok payload:

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
	 }
	
   ]
 },
 "metadata_version": "1"
}

```


