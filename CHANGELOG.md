# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### [0.0.12] - 2023-02-08
  - Fixed Fortify Vulnerability scans issues
  - Enhanced MMG Validation with following rules:
     - OBX 3 + OBX-4 must be unique
     - OBX-4 must be present for grouped elements
  - Added HTTP endpoints for structure and mmg-validation for QA



### [0.0.11] - 2023-01-25
  - Parameterized Azure environment specific variables on pom files of Azure function projects to help with CI/CD
  - Finalized upgrade of NIST library for v. 1.5.6 and scala 2.13
  

### [0.0.10] - 2023-01-11
 - Added message_info to metadata payload with relevant information from message, including routing.
 - Updated mmg-based-transformer with exception handling for bad format event hub messages or transformations exceptions.
 - Updated mmg-sql-transformer with exception handling for bad format event hub messages or transformations exceptions.
 - Updated mmg-sql-transformer for messsage_profile_identifier changed from tables to flat model.
 - Updated all functions for dev for function names based on new teraform config and added sample reports in all functions.
 - Upgraded Nist Libraries for scala 2.13 and retrofitted upgrade to our own lib-nist-validator.
 - Created first draft of Lab Template MMG configuration.
 - Fixed bug where JSON was supressing elements with NULL values in lib-dex-commons.

### [0.0.9] - 2022-12-14
 - Renamed mmg-processor module to mmg-based-transformer
 - Created function to load Condition2MMGMapping table
 - Created new function fn-mmg-sql-transformer which transforms the output of the fn-mmg-based-transformer, which includes all MMG elements: singles, singles repeated, and repeated blocks.
 - Designed Data consumption routing and implemented changes on metadata. (Need to implement receiver-debatcher)
 - Updated Receiver/Debatcher to throw invalid messages into the Error queue (hl7_recdeb_err)
 - Created Readme to several modules - lib-dex-common, lib-nist-validator, Receiver/Debatcher, Structure Validator
 - Updated getMMGs() to throw error if condition is not present on table and created method to retrieve a list of MMG names used by a given scenario (event_code, profile, reporting_jursidiction)

### [0.0.8] - 2022-11-30
  - Created Hepatitis MMGs
  - Created PhinSpec 3.2 profiles for NIST Structure Validation
  - Updated MMG-Based Transformer (fn-message-processor) to generate HL7-specific field elements and added PHIN-Vads concept name and preferred concept names.
  - Bug Fixes
    - Fixed Debatcher Code broken due an previous update
    - MMGReport status was printing the entire report instead of just the status.   
    - Missing entries in Redis for Value Set
    
### [0.0.7] - 2022-11-16
  - Added new features to lib-dex-commons: RedisProxy & String normalization function.
  - Added ability to load legacy MMGs into Redis. 
  - Added method to retrieve appropriate MMGs based on HL7 message for Validation and transformation.
  - Updated AZ Functions to properly populate summary metadata.
  - Added Hepatitis support for structure and MMG validation.
  - Created Message Processor to transform HL7 into MMG-Based schema.

## [0.0.6] - 2022-11-02
  - Created Databricks notebooks to ingest Event Hub messages.
  - Updated all functions (fn-structure-validator, fn-mmg-validator, fn-receiver-debatcher) to be deployed and running on AZ
  - Updated metadata on all functions (see above) based on design - #105 .
  - Created lib-dex-commons library for sharing code among functions/services.
  - Updated fn-structure-validator to be kotlin (instead of scala)
  - Replaced hl7-structure-validator (scala) with lib-nist-validator (Kotlin)
  - Updated PhinVads for error management.
  - Created Azure Data Explorer to ingest Event Hub messages for monitoring and debugging.
  - Updated Legacy Arbo MMG.
  - Created Unit tests for structure and mmg validation.
  
## [0.0.5] - 2022-10-19
 - Added Vocabulary validation to MMG Validator.
 - Added all Legacy MMGs and both GenV1.
 - Added Unit test for GenV1 messages.
 - Changed build and configuration files for Receiver/Debatcher for Deployment.
 - Changed Structure Validator to use basic Json Objects instead of POJOs.
 
## [0.0.4] - 2022-10-05
- Created MMG Validator Function.
- Created tool to generate MMG configurations  
  - Created GenV1 MMG configurations ( Case and Summary).
- Fixed Structure Validation Report serialization.
  
## [0.0.3] - 2022-09-22
- HL7 Structural Validator: added PHIN 2.0 and 3.0 profiles, updated to validate based on profile requested
- Added Phin Spec 2.0 and 3.0 IGAMT Profiles.
- HL7 validation function to structure validate messages
- Started on HL7 Message Processor function
- Updated Phin VADS function to be based on schedule.
- Started on MMG Loader function.

## [0.0.2] - 2022-09-12 
 - Developed Receiver-Debatcher function
 - Developed phin-vads function
 - First Graphana dashboard with basic infrastructure metrics

## [0.0.1] - 2022-08-29

### Added
- Case-based surveillance data pipeline proof-of-concept for Spark DataBricks
- HL7 Messages structural validator 
- Docs: How to create development environment for Azure functions project
- Tools: Various HL7 development tools or experiments
- Folders for future tools: mmg, phin-vocab, and fns-hl7-pipeline
- Phin-vocab function for fetching the value sets from phinvads api.
