# HL7 v2 Validation API

The HL7v2 Validation API is designed to assist public health agencies in validating messages
against the applicable profiles and standards as they prepare to submit their data to CDC.

The purpose of validating messages via the API is to "pre-test" the messages 
to see whether they will pass the structure validation part of the DEX HL7 v2 pipeline.
The API is capable of validating a single message or a batch of up to 100 messages and provides immediate feedback.
Using this tool, implementers are able 
to discover any errors or conformance issues with the way their test
messages are constructed prior to going live with submission of HL7 v2 messages to the DEX pipeline.

## Single message submission
If a single message is submitted to the validator, the full report of structural errors and warnings
is returned back to the caller. This detailed report includes the location and description of
each error and warning found within the message.

## Batch submission
If a batch of concatenated messages are submitted to the validator, a summary report of errors found is returned. 
This summary provides only error counts -- how many total errors were found in the batch, 
and groupings of errors by type, category, and HL7 path. It also provides the 
total error count for each individual message in the batch by message number.

### Summary report error counts
#### By Type 
This groups the errors by validation type. On the NIST conformance profiles, there will be structure, 
content and value-set errors. The "By Type" grouping will provide a list of the errors of each of those types for all messages.

#### By Category
Each error in the NIST conformance profile is associated with a category. 
This grouping will provide a list of the errors in each category, if any, for all messages. 
Possible values include but are not limited to "Constraint Error", "Usage", and "Length".
If no errors were encountered for a category, that category will not be listed.

#### By Path
The HL7 path to an error includes at least the segment and field where the error occurred.
If applicable, the component, subcomponent, or repetition number may also be included in the path.

For example, the following indicates that one error was found in the first repetition of the third field of the first PID segment:

    "PID[1]-3[1]": 1


### Sample Summary Report

``` json
{
  "total_messages": 5, //Total Number of Messages received
  "valid_messages": 2,  //Number of messages that passed validation with 0 errors. (warnings allowed)
  "invalid_messages": 3, //Number of messages that failed validation with at least one error encountered.
  "error_counts": { 
    "total": 4,  //Total number of errors on all messages validated
    "by_type": {
       "structure": 1,
       "content": 2,
       "value_set": 0,
       "other": 1
    },
    "by_category": {
       "Constraint Failure": 2,
       "Runtime Error": 1,
       "Usage": 1
    },
    "by_path": {
       "PID[1]-3[1]": 1,
       "OBR[1]-22[1].1": 1,
       "MSH-12": 1,
       "PID[1]-5[1]": 1
    },
    "by_message": {
       "message-1": 2,
       "message-2": 1,
       "message-3": 1,
       "message-4": 0,
       "message-5": 0
    }
  }
}

```
## Sample Detailed Report

```json 
{
    "entries": {
        "structure": [
            {
                "line": 3,
                "column": 1,
                "path": "ORC[1]",
                "description": "Segment ORC (Common order segment) has extra children",
                "category": "Extra",
                "classification": "Warning",
                "stackTrace": null,
                "metaData": null
            },
            {
                "line": 3,
                "column": 196,
                "path": "ORC[1]-14[1]",
                "description": "The primitive Field ORC-14 (Call Back Phone Number) contains at least one unescaped delimiter",
                "category": "Unescaped Separator",
                "classification": "Warning",
                "stackTrace": null,
                "metaData": null
            }
        ],
        "content": [],
        "value-set": []
    },
    "error-count": {
        "structure": 0,
        "value-set": 0,
        "content": 0
    },
    "warning-count": {
        "structure": 2,
        "value-set": 0,
        "content": 0
    },
    "status": "VALID_MESSAGE"
}

```
## How to Submit A Message for Validation
Messages can be submitted to the Validation API using any HTTP communication tool, e.g., Postman, Fiddler, etc.
To submit successfully, callers must supply the message type as a query parameter. Valid message types are "CASE" or "ELR".
"CASE" messages conform to a Message Mapping Guide (MMG) and PHIN Specification, while "ELR" messages conform to a 
specification that constrains the HL7 ELR Implementation Guide for a specific condition or set of conditions tracked by 
the CDC. When the message type is "ELR", the condition-related guide indicator must also be supplied, using the 
query parameter "route". Valid values for "route" include "COVID19_ELR", "PHLIP_FLU", "PHLIP_VPD", and "DAART".

For the message body, an HL7 message or batch can be submitted as "raw" plain text, or a file containing the message
or message batch can be uploaded as a binary attachment.

Note that ELR batch messages are expected to comprise messages that all conform to the same implementation profile or "route".
CASE batch messages may contain messages that conform to various MMG profiles.

### Example Submission URLs
CASE message(s):  
    
    https://ocio-ede-tst-svc-hl7-validation.azurewebsites.net/validate?message_type=CASE

ELR message(s):

    https://ocio-ede-tst-svc-hl7-validation.azurewebsites.net/validate?message_type=ELR&route=COVID19_ELR