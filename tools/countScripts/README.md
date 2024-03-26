# INTRO

The scripts on this folder help count files that have been creeated/moved by DEX pipeline.

Currently there are two scripts:

## countFiles

This script will count all files for a given day passed as parameter (right now 2024 is hardcoded).

param: mm/dd 

  - pass the month and day always as two digits to count files for that given day.

The script will count files for:
  - upload storage account: ingress of files, HL7 and CSV files
  - Routing storage account:
    - ingress of upload files, HL7 and CSV files
    - HL7 output for all functions - recdeb_reports, redaction reports, validation reports, hl7 json and lake of segments transformations.
    - routing dead letter - will count how many files got moved to dead letter.
  - EZDX storage account: files provisioned to ezdx for that given day.

The script will also call some Processing status endpoints to get some counts of reports submitted:
  - Number of HL7 uploads done
  - Number of CSV uploads done
  - Number of reports submitted that failed structure validation.

## countHours

   This script will count the ingress of upload and routing files for the day, but will count the HL7 outputs by hour and compare it with files copied to ezdx

  param: mm/dd 

  - pass the month and day always as two digits to count files for that given day.
   The output of the hl7 reports looks like this:

  ```
$ . ./countHours.sh 03/25
@@@
@@@ Counting routeingress
@@@
Counting HL7
hl7 -> 11936
Counting csv
csv -> 106
@@@
@@@ Counting hl7 outputs
@@@
00 -> 11 11 11 11 11 ==> 11 11 11 11 11
01 -> 11 12 12 10 10 ==> 11 12 12 10 10
02 -> 14 14 14 14 14 ==> 14 14 14 14 14
03 -> 13 13 13 13 13 ==> 13 13 13 13 13
04 -> 19 19 19 19 19 ==> 19 19 19 19 19
05 -> 11 12 12 11 11 ==> 11 12 12 11 11
06 -> 108 911 911 815 815 ==> 108 911 911 815 815
07 -> 32 32 32 32 32 ==> 32 32 32 32 32
```

The hl7 outputs lists:
{HOUR} -> {HL7 outputs} ==> {EZDX provisioned}

where
* {HOUR} is the 24 hour format, UTC based hour for the line
* {HL7 outputs} is a space separated list of all reports: RECDEB REDACTION VALIDATION HL7-JSON LAKE-SEG
* {EZDX provisioned is a space separated list of all files copied to ezdx on the same order as hl7 outputs: RECDEB REDACTION VALIDATION HL7-JSON LAKE-SEG

