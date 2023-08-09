# CDC Data Exchange HL7

HL7 pipeline functions project: receiver-debatcher, redactor, structure-validator,  lake-segments-transformer, json-lake-transformer

#### HL7 receiver-debatcher pipeline function - Receives HL7 data that gets uploaded to DEX and makes it available for processing via HL7 pipeline(Case notification messages and ELR)
- Code for a receiver-debatcher function -  proof-of-concept: - proof-of-concept:
[receiver-debatcher](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-receiver-debatcher)

#### HL7 redactor pipeline function - Redacts PII data from the message
- Code for a redactor function -
[redactor](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-redactor)

#### HL7 structure-validator function - validates HL7 structure using PHIN Spec profiles created in the IGAMT tool
- Code for a structure-validator function - 
[structure-validator](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-structure-validator)

#### DEPRECATED ~HL7 mmg-validator pipeline function - Uses MMG profiles to validate HL7 message content~ 
- Code for a mmg-validator function - proof-of-concept:

#### DEPRECATED ~HL7 mmg-based-transformer pipeline function~
- Code for a mmg-based-transformer function - proof-of-concept:

#### DEPRECATED ~HL7 mmg-sql-transformer pipeline function~
- Code for a mmg-sql-transformer function - proof-of-concept:


#### HL7 lake-segs-transformer pipeline function - Uses HL7-PET library to parse the HL7 message(lake of segments) in a way that understands segments hierarchy
- Code for a lake-segs-transformer function:
[lake-segs-transformer](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-lake-segs-transformer)

#### HL7 json-lake pipeline function - Creates Json model based on HL7-PET profiles with segment hierarchy
- Code for a json-lake function:
[json-lake](https://github.com/CDCgov/data-exchange-hl7/tree/develop/fns-hl7-pipeline/fn-hl7-json-lake)

