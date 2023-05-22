
## MMG-Validator Function for the HL7 Pipeline

# TL;DR>

This service can validate an HL7 message content-wise using MMG profiles in the format created by the [MMG-AT]()tool.

# Details

The MMG Validator service validates messages against their specific MMG(s). Currently, the service supports the following MMGs:

* Arboviral V1.2
* Arboviral V1.3
* Babesiosis
* Babesiosis V1.1
* Congenital Rubella Syndrome
* Congenital Syphilis
* Congenital Syphilis V1.1
* Covid-19
* Covid-19 V1.1
* Covid-19 V1.2
* Gen Case Map V1.0
* Generic MMG V2.0
* HAI MDRO Candida Auris
* HAI MDRO CP-CRE
* Hepatitis V1.0 MMG Core
* Hepatitis V1.0 MMG Hepatitis A Acute
* Hepatitis V1.0 MMG Hepatitis B Acute De
* Hepatitis V1.0 MMG Hepatitis B Chronic De
* Hepatitis V1.0 MMG Hepatitis B Perinatal De
* Hepatitis V1.0 MMG Hepatitis C Acute De
* Hepatitis V1.0 MMG Hepatitis C Chronic De
* Lab Template V3
* Lyme Disease
* Malaria
* Measles
* Mumps
* Pertussis
* Rubella
* STD
* STD V1.2
* Summ Case Map V1.0
* TBRD
* Tuberculosis and LTBI V3.0.3
* Tuberculosis Message Mapping Guide V2.03
* Varicella Message Mapping Guide V3.0.1
* Varicella Message Mapping Guide V2.0.1
* Varicella Message Mapping Guide V1.0
* Trichinellosis
* FDD Cryptosporidiosis
* FDD Cyclosporiasis
* FDD Cholera And Vibriosis
* FDD Foodnet
* FDD Campylobacteriosis
* FDD Salmonellosis
* FDD Stec
* FDD Shigellosis
* FDD S.Typhi And S.Paratyphi

The following rules are checked against each HL7 message:

* **Cardinality** - validates that the number of answers in the message matches the cardinality specified in the MMG for that specific field
* **Data Types** - validates that the data type in OBX-2 matches the data type defined by the MMG
* **Vocabulary** - for CE and CWE fields, validates codes provided in OBX-5 against Value Sets from [PHIN VADS](https://phinvads.cdc.gov/vads/SearchHome.action).
* **Invalid OBX segments** - if an OBX-3 code is present in the message that is not mapped in the MMG, it will be flagged as a Warning
* **Observation Sub ID (OBX-4)** - Makes sure Observation Sub ID is populated for repeating elements of the MMG
* **Dates** - Make sure dates are sent in appropriate HL7 format and are valid date values
* **MMWR Year** -Validates that MMWR Year is a valid year (this violates our pattern of fully configurable rules; this one is hardcoded but was requested by customer to be included)



![image](https://user-images.githubusercontent.com/3239945/208454032-b4169ed4-1a48-41c5-a603-20f8fbf6631e.png)


## Pipelne:

Input: hl7-structure-ok

Output: hl7-mmg-validation-ok ; hl7-mmg-validation-err

## Hl7-mmg-validation-ok payload:

``` json
 "content": "Base64(MSH|^~\&|....)",
 "message_info": {
    "event_code": "10110",
    "route": "hepatitis_v1.0_mmg_hepatitis_a_acute",
    "mmgs": [
      "mmg:generic_mmg_v2.0",
      "mmg:hepatitis_v1.0_mmg_core",
      "mmg:hepatitis_v1.0_mmg_hepatitis_a_acute"
    ],
    "reporting_jurisdiction": "48",
    "type": "CASE"
  },
 "meta_message_uuid": "",
 "summary": {
    "current_status": "STRUCTURE_VALID"
 },
 "metadata":
    "provenance": {
	{unchanged}
       },
     "processes": [
	 {
		 "process_name": "Receiver",
		 "start_processing_time": "2022-10-01T13:00:00.000",
		 "end_processing_time": "2022-10-01T13:01:00.000",
		 "process_version": "1.0.0",
		 "eventhub_queued_time": "2022-10-01T13:04.000",
		 "eventhub_offset": 1234567890,
		 "eventhub_sequence_number": 123,
		 "status": "SUCCESS"
	 },
	 {
		 "process_name": "Structure-Validator",
		 "start_processing_time": "2022-10-01T13:02:00.000",
		 "end_processing_time": "2022-10-01T13:03:00.000",
		 "process_version": "1.0.0",
		 "eventhub_queued_time": "2022-10-01T13:04.000",
		 "eventhub_offset": 1234567890,
		 "eventhub_sequence_number": 123,
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
		 "eventhub_queued_time": "2022-10-01T13:04.000",
		 "eventhub_offset": 1234567890,
		 "eventhub_sequence_number": 123,
		 "status": "SUCCESS",
		 "configurations": [
		 	"genV1",
			"Hep_v1.0"
		],
		"report": {
		  {full MMG Validation Report}
		}
	 }
   ]
 },
 "metadata_version": "1"
}

```


