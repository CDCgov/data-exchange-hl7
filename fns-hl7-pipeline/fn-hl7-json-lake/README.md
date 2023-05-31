## HL7-JSON-Lake Function for the HL7 Pipeline

# TL;DR>

This service creates a HL7 JSON Lake from a given HL7 message.


# Details:

## Lake of Segments schema:

Each segment in the HL7 message gets broken down into the following fields:

- **Segment_number**: a sequential number of that given segment, matching the line number.
- **Segment**: The actual segment this record represents. The full string with the entire segment is present here.
- **Parent_segments**: The full hierarchy of the parent segments for the segment represented in this record. For MSH segments, parent_segment will be NULL. All other segments should have at least one parent.

Ex.:
- Parent segment of a PID is the MSH.
- Parent segment of an OBR is the MSH.
- Parent segment of an OBX is the MSH and the OBR.

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
  
