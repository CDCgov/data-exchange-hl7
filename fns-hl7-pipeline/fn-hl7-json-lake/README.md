## HL7-JSON-Lake Function for the HL7 Pipeline

# TL;DR>

This service creates a HL7 JSON from a given HL7 message.


# Details:

## HL7 Json Model

The Json model is created based on HL7-PET profiles that indicates the segment hierarchy

The hierarchy of segments is fully configurable. Currently, we're using the following configuration:

```json
{
  "segmentDefinition": {
    "MSH": {
      "cardinality": "[1..1]",
      "children": {
        "PID": {
          "cardinality": "[1..1]",
          "children": {
            "NTE": {
              "cardinality": "[0..*]"
            },
            "PV1": {
              "cardinality": "[0..1]"
            },
            "NK1": {
              "cardinality": "[0..1]"
            }
          }
        },
        "ORC": {
          "cardinality": "[0..1]"
        },
        "OBR": {
          "cardinality": "[1..*]",
          "children": {
            "OBX": {
              "cardinality": "[0..*]",
              "children": {
                "NTE": {
                  "cardinality": "[0..*]"
                }
              }
            },
            "SPM": {
              "cardinality": "[0..1]",
              "children": {
                "OBX": {
                  "cardinality": "[0..*]"
                }
              }
            },
            "NTE": {
              "cardinality": "[0..*]"
            }
          }
        }
      }
    }
  }
}

```


## Pipelne

- Inputs:
	- hl7-structure-ok (ELR messages)
- Outputs:
	- hl7-json-lake-ok
	- hl7-json-lake-err

![image](https://github.com/CDCgov/data-exchange-hl7/assets/3239945/b0ea7c79-bef5-40dd-ae0b-aaf49ae6171e)

## Transform Requirements

DEX created a transformer based on CELR requirements where an HL7 message is mapped to JSON. CELR hardcoded their needs to a single use case that satisfied their needs. 

DEX had to support CELR use case, but also be able to support other use cases within CDC and therefore needed a more generic solution to transforming HL7 into JSON.

The guiding requirements for DEX HL7-JSON Transformer were as follows:

* Be generic in such a way that it can work for any HL7 message.
* Be able to be customizable for different HL7 message types
* Reduce the need to be a HL7 SME for consumers of the JSON.
* Do not loose information - all information present on HL7 must be represented on the JSON output
* Be performant with low memory footprint (i.e., cloud ready for million message scales)

With the above requirements in mind, we enhanced a library we used on previous projects - lib-bumblebee to support this new use case.

Lib-bumblebee was created to transform HL7 into JSON based on a pre-defined template and a HL7 Profile, and uses HL7-PET to extract information out of the HL7 to populate the  JSON template. This template is very program specific and also, it has the tendency of losing information that is not directly mapped on the template.

For that reason, we created a new transformer that is not based on a given template, but only based on the  HL7 Profile. The profile injects knowledge about the HL7 into the transformer, such that:
* It determines the segment hierarchy (for example, OBX are children of OBR)
* Field names, data types and cardinality

Currently, the HL7-JSON uses a single profile for all messages received. But in the event some program needs to tweak their JSON in such a way that is not compatible with existing output, this can be accomplished by updating the Profile.

The Profile is native to the HL7 PET, i.e., the HL7-PET library knows how to parse and understand a profile and use it for multiple functionalities, one of them, to understand segment hierarchy, which is very important part of our transformation.

![image](https://github.com/CDCgov/data-exchange-hl7/assets/3239945/abdefa7d-8852-48fd-9a81-6a9d10c5dbb4)

In the example above, MSH is our only root-segment, i.e., a segment that does not have any parent. All other segments are defined as a direct child of MSH, or indirectly being a child of a MSH child. For example, the OBX segment is defined as a direct child of OBR, which is a child of MSH.
Also, a Segment can be child of multiple parents. In the example snapshot above, we can see "NTE" segment being a child of OBX and OBR (also PID, but for brevity, it is hidden in the  {… } above.

The second piece of information defined on such profiles is the fields of each segment, their types and cardinalities, i.e., how many times can the field repeat within the message. The cardinality is very important because it will dictate whether the field will be represented as a simple JSON Object (when cardinality is 1) or if will be represented as an array, when cardinality is greater than 1, say 3 or * (unlimited).

Here's an example of the MSH fields:

![image](https://github.com/CDCgov/data-exchange-hl7/assets/3239945/c7d2807a-6914-4024-b85d-ee4606c4a4f3)

On the profile above, we see the first 7 fields of an MSH segment. The field number provides the position of that field. Say fieldNumber: 5, represents the value of MSH-5. 

**NOTE**:
As you can see, MSH is a special segment, since MSH-1 is the field delimiter (usually a pipe - |), while all other segments the pipe is simply a given and the first field of all other segments will be the value of the first field after the first pipe. (But all that is taken care of by hl7-pet, as long as the profiles abides by those rules).

The name will be used to generate the attribute name on the JSON after some normalization - This normalization includes lowercasing the entire name and replacing any special characters, including spaces into a underscore- "_".

The data type will indicate how to populate this attribute - if the type is a "primitive" data type, i.e., a data type that cannot be decomposed any further, then, it will be represented as a primitive value on the JSON.
Since JSON does not support true date/time fields and those are usually saved as Strings, on the example above, field "7" will be transformed like this:

```json
    "date_time_of_message": "20230102120102000" 
```

In the event that the data type is a complex data type, i.e., a data type that can be further decomposed, the transformation will look into how that data type is defined and further decomposed. This process is recursive, i.e., if the data type itself has a complex data type, that will be further decomposed creating a second layer of json object. In the example above, fieldNumber: 3 is defined as HD. Based on our Profile, we can see HD fields defined as having 3 components:

![image](https://github.com/CDCgov/data-exchange-hl7/assets/3239945/621ffbc9-8954-4493-8815-115d055e8807)

All those components are currently treated as "primitives", including ID (more on this later) and the final transformation will be something like this:

```json
      "sending_application": {
	     "namespace_id": "STARLIMS.AR.STAG",
	      "universal_id": "2.16.840.1.114222.4.3.3.2.5.2",
	      "universal_id_type": "ISO"
     }
```

**NOTE**:
A "primitive" field is actually identified by the lack of further decomposition of another field. In other words, complex fields are defined on our profile. If a given data type is not defined on the profile, it will be treated as a "primitive".
For example, you will not find "ST", "DT", "TM", etc.

Some fields in HL7 are defined as having a single component. Ex.: the data type ID is defined as:

![image](https://github.com/CDCgov/data-exchange-hl7/assets/3239945/753599c8-ea6b-4b6a-84e6-ebc80004f3b6)

For our purposes, we decided to treat those types as "primitives", i.e., they will not be decomposed like:

PID-24, Multiple birth indicator, is defined as ID type. If we would like to decompose it, it will be represented as:

```json
"multiple_birth_indicator": {
	"coded_value_for_hl7_defined_tables": "1"
}
```
Instead, since ID always have a single subcomponent, the profile does not define it and the final JSON is generated as:

```json
"multiple_bith_indicator": "1"
  ```

### Cardinality:

Because sending application cardinality is defined as [1..1], you can see that sending_application value is a json object (single value) instead of array.

When cardinality is multiple, like MSH-21 on PHIN Guide Profiles is defined as [2..3], you could see something like this on the output JSON:

```json
     "message_profile_identifier": [
	      {
		"entity_identifier": "NOTF_ORU_v3.0",
		"namespace_id": "PHINProfileID",
		"universal_id": "2.16.840.1.114222.4.10.3",
		"universal_id_type": "ISO"
	      },
	      {
		"entity_identifier": "Generic_MMG_V2.0",
		"namespace_id": "PHINMsgMapID",
		"universal_id": "2.16.840.1.114222.4.10.4",
		"universal_id_type": "ISO"
	      },
	      {
		"entity_identifier": "Lyme_TBRD_MMG_V1.0",
		"namespace_id": "PHINMsgMapID",
		"universal_id": "2.16.840.1.114222.4.10.5",
		"universal_id_type": "ISO"
	      }
	]
```
	

Even if a message contains a single value, if the profile identifies a field as been able to support multiple values (i.e., Cardinality is greater than 1), that field will always be an array:

```json
 "message_profile_identifier": [
      {
        "entity_identifier": "PHLabReport-NoAck",
        "namespace_id": "phLabResultsELRv251",
        "universal_id": "2.16.840.1.113883.9.11",
        "universal_id_type": "ISO"
      }
]
```

One premise is that the HL7-JSON transformer will receive structurally valid HL7 messages (based on our message pipeline, the message has gone through validation and only valid messages are propagated down the pipeline. If they are not valid, they are stopped and do not reach our transformation.
 

### Lossless of Information:

One of the requirements guiding the development of the HL7-JSON transformer, is to be lossless. In other words, all information present on the HL7 should be somehow represented in the output JSON. 
In summary, we can achieve this, but with a caveat. The driver of the transformation is the profile being used. Therefore, HL7 JSON can only transform what the profile knows about. 

With that said, the transformer can indeed lose information in the following scenarios:
*  The HL7 message has an unexpected segment - a segment not mapped on the profile
* A given segment has more fields than fields mapped by our profile.
* A field with cardinality of [0..1], or [1..1] (a field that can have only one value) is sent with a repeating value (the repeating value will be ignored by the transformer.







  
