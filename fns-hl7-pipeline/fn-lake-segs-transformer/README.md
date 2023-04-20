
# TL;DR>

This service creates a Lake of Segments from a given HL7 message.
	
	
# Details:

One of DEX data products is the Lake of Segments. A Lake of segment is the closes representation of a HL7 message. It will contain all of the HL7 information. The only transformation done here is that each segment becomes its own record on the lake.

It uses the  HL7-PET library to parse the message in such a way that it understands the hierarchy of segments. For example, a OBX is really a child of a Parent OBR.
	
## Lake of Segment schema:

Each segment on the HL7 message gets broken down into the following record:

- **Segment_number**: a sequential number of that given segment, matching their line number.
- **Segment**: The actual segment this record represents. The full string with the entire segment is present here.
- **Parent_segments**: The full hierarchy of the parent segments for the segment represented on this record. For MSH segments, parentsegment will be NULL. All other segments should have at least one parent.

Ex.: 
- Parent segments of a PID is the MSH.
- Parent segment of a OBR is the MSH.
- Parent segment of a OBX is the MSH and the OBR.

The hierarchy of segments is fully configurable. Currently, we're using the following configuration:

```
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
  - hl7-lake-segments-ok
  - hl7-lake-segments-err

![image](https://user-images.githubusercontent.com/3239945/233420469-18905887-88ed-4181-b80e-8922591ae92d.png)
  
