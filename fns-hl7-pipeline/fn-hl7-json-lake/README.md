## HL7-JSON-Lake Function for the HL7 Pipeline

# TL;DR>

This service creates a HL7 JSON Lake from a given HL7 message.


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
	- hl7-mmg-validation-ok (CASE messages)
	- hl7-structure-elr-ok (ELR messages)
- Outputs:
	- hl7-json-lake-ok
	- hl7-json-lake-err

![image](https://user-images.githubusercontent.com/3239945/233420469-18905887-88ed-4181-b80e-8922591ae92d.png)
  
