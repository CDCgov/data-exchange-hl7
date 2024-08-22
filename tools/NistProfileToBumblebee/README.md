# NistProfileToBumblebee

## Introduction
The NistProfileToBumblebee project was designed to ease the creation 
of configuration profiles for the [lib-hl7v2-bumblebee](https://github.com/CDCgov/lib-hl7v2-bumblebee)
library. It takes as input an IGAMT validation profile XML file and outputs 
the two configuration JSON files needed by the HL7JsonTransformer to encode HL7
message data into JSON format.

## Background

### Structure Validation in DEX
Users of the DEX HL7v2 Pipeline are familiar with the fact that the step that performs
structure validation of HL7v2 messages uses NIST IGAMT profiles. 
Each NIST profile is specific to a use case:
a particular HL7 message type and version as well as structural requirements 
set by the receiving program. The NIST profiles are exported from the IGAMT tool into XML 
files. These XML profiles are then used by the structure validator in the 
HL7v2 Pipeline to ensure that messages conform to HL7 and program-specific requirments.

### Transformation in DEX
As the last step in the HL7v2 Pipeline, the validated HL7 message is transformed into JSON.
The pipeline function that performs this transformation, the HL7 JSON Lake Transformer, uses the lib-hl7v2-bumblebee
library to execute the transformation. The library requires two (2) configuration files
to determine the structure of the output:
a message structure profile that details the segment hierarchy and included fields,
and a data types profile that details the components of the data types to include in the output.
(A thorough explanation of these configuration files can be found 
in the [lib-hl7v2-bumblebee documentation](https://github.com/CDCgov/lib-hl7v2-bumblebee).)


### Rationale for NistProfileToBumblebee Tool
Creating the configuration files needed by the lib-hl7v2-bumblebee library is an arduous 
task if done by hand. Because the IGAMT PROFILE.xml file contains all of the information
needed for these transformation profiles, and because it is likely that receiving programs
would want the same structure that was validated to also be the structure used for output,
it seemed natural to create a tool that would transform the PROFILE.xml data
into the structure and data type JSON configuration files.


## Usage
### Standalone Execution
The NistProfileToBumblebee tool can be used as a standalone executable jar
that is run from the command line. As such, it takes up to 3 parameters:
1. The full path to the PROFILE.xml file
2. The path to the folder where the JSON output should be saved
3. An optional 3rd parameter, the base name of the output JSON profile. If provided, 
the two files saved to the output folder will have this base name, prepended with 
"profile-" for the structural profile and "fields-" for the data type fields profile.
If not provided, the value of the "Name" attribute in the Metadata element of the 
PROFILE.xml will be used as the base name.

Example usage:
```
> java -jar NistProfileToBumblebee.jar C:/Profiles/PROFILE.xml C:/Profiles/output MyNewProfile
```
Note that regardless of operating system, the tool requires forward slashes `/` to separate
directory names.

### Use In Other Programs


## Build

## Dependencies