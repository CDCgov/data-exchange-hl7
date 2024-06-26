# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### [0.0.48] 2024-06-26
	- Enhanced all FN health checks to check dependencies.
	- Improved Redactor when redacting fields with multiple values 
 	- Separated PHLIP into two data streams: PHLIP_FLU and VPD
 	- SImplified redactor profile loading by data_stream_id

### [0.0.47] 2024-06-12
	- Added version to Routing Metadata
	- Created DockerFile for all Functions

### [0.0.46] 2024-05-29
	- Added Fields to PID up to 35
	- Included Fortify scans for new modules
	- Dockerized two functions for AKS migration path
 	- Removing binary data from HL7 message and saving it as a separate file.

### [0.0.44] 2024-05-16
		- Removed Process Status Traces from functions
		- Added sender_id and received_filename to metadata
		- Keep supporting metadata when file sink is copying files
		- Throw unknown exceptions on all functions for Azure retries
		- Saving partitioned folders by date using new dex_ingest_datetime

### [0.0.43] 2024-04-17
	- Moved AzureBlobProxy from receiver-debatcher to lib-dex-commons (to be reused by other Fns)
	- Created bash scripts to deploy Azure Functions with Zip deploy instead of maven plugin.

### [0.0.42]  2024-04-03
	  - Modified file-sink to create blobs and metadata in single call.
	  - Added retries on file sink when upload fails
	  - Improved Structure Validator status when critical failures happens (as when no profiles match message) 
	  - Fixed issues with NRSS validation profile
	  - Added health checks for resource dependencies on lib-commons
     - Added retries on Receiver/Debatcher for loading metadata appropriately


### [0.0.41] - 2024-03-20
	- Added retries to read metadata out of blob files (Azure SDK bug?)
	- Bug fixes - fixed Debatcher report on counts
	- Added eventhub health check on lib-dex-commons
 	- Added configuration to file-sink to customize output directories.

### [0.0.40] - 2024-03-06
	- Fixed some typos on PhinGuideProfile used to generate HL7 Json
	- Implemented  Metadata V2 on all functions.
	- Changed Receiver Fn to Read events from Storage Queue instead of EH to avoid duplication
	- All functions now provide the actual stage.version instead of hardcoded
	- All PS Reports have unique schema names (PS Requirement)
	- Improved Receiver/Debatcher Reporting schema to be standard with other stages.


### [0.0.39] - 2024-02-21
	- Setting batchCheckpointFrequency to 1 on all Pipeline functions
	- Refactored all functions to reuse azure resource connections - event hub, service bus, cosmos db, storage account, etc.
	- Added health check to all functions and sidecar functions and configured Health Checks on DEV
	- Removed transformation Jsons from Processing status report (hl7-json and lake of segments)
	- HL7-Json now no longer serialize attributes with no values
	- Replaced processes array on metadata for a single Stage attribute (remove duplication of information on payloads)
	- Refactored Receiver debatcher to be called from EventGrid events to avoid the Event hub duplication (Receiver cannot afford to receive duplicate events, otherwise the data will be fully duplicated across the pipeline with a new message-uuid.
	- Improved EventHub sender to bypass messages that are too big (and Report on them accordingly
	- Moved transformation output from "report" node to "output" node in JSON


### [0.0.38] - 2024-02-07
	- Upgrade azure-functions maven plugin due to strange bug
	- Upgraded azure Bom to 1.2.20 due to NPE bug
	- Fixed logging on file-sink functions
	- Added extra metadata to Debatcher report
	- Updated HL7 Json profile for
		○  multiple patient ids
		○ Changed file_separator to field_separator for MSH-1
	- Created a Covid19 specific config for redaction
	- Added /health endpoint to Debatcher Fn
	- Created ability to Redact based on a expression for Covid requirements
	- PHLIP-VPD fixes
	- Improved Structure validator error messages when missing MD
	- File-sink bug fix on date folders.
		○ Removed content from payload.
	- Using Static Event hub clients for better connection management.


### [0.0.37] - 2024-01-24

	- Updated Validation API documentation to include support for DAART
	- Added validation profiles for NRSS
	- Bug fixes on PHLIP FLU profiles
	- Fixed a bug on lib-bumblebee for primitive types and cardinaltiy > 1
	- Included meta_ext_uploadid as part of metadata
	- Updated the Debatching report for Receiver-Debatcher Function with appropriate metadata and deployed file-sink sidecar function to provision the report to programs.
	- Deployed Processing Status Reports sidecar to submit report to PS API


### [0.0.36] - 2024-01-10
	- Improved Lake of Segments schema to have a segment_id based on HL7 Path, and parent_segments to reference segment_id ordered from parent to grand-parent all the way to root.
 	- Upgraded Azure bom to 1.2.18
   	- Creating sidecar function to send appropriate Reports to Processing Status API
    	- Fixed metadata to remove duplicate config/configs and remove serialized eventHubMetadata 
     	- Including DEX-Upload metadata on HL7 metadata (upload_id, destination_id, event)
      	- Deployed DAART profiles to Structure Validator
       	- Updated File-sink sidecar function to support sending data to Routing, using config to know folder name and default to partition files by yy/mm/dd subfolders.
	- Modified Redaction Config of PID-5 to not reject messages that send data on PID-5[1] instead of PID-5[2]
 	- Performed bug fixes on PHLIP FLU Profiles
  	- Generating a Report for Receiver Debatcher indicating number of messages received per file.
  	

### [0.0.32] - 2023-11-15
	- Implemented a CosmosDB client on lib-dex-commons used by cosmos-sink sidecar
	- Configured Elastic functions to scale beyond 20 instances
	- Created File sink side car to save events to storage account
	- Created function to move files between containers (for performance testing)
	- Deployed Branching sidecar fn and reconfigure pipeline functions to output events to a single eventhub.
	- Added Constants.kt to lib-dex-commons

### [0.0.31] 2023-11-01
	- Cleaned out CosmosDBOut annotations on all Functions.
	- Improved Open API documentation for Validation API
	- Added segment_id to Lake of segments and listing parentage using segment_id from parent to Grandparent order.
	- Add Error Response JSON to Validation API
	- Updated Validator API Postman collections
	- Created sidecar function to handle branching of good/bad messages
	- Created a CosmoDB Proxy on Lib-dex-commons for standardization on using cosmos db
	- PHLIP_FLU bug fixes:
		- MSH-21[1] missing.
	- Performance Testing:
		- Moved all Functions to Elastic Plans
		- Changed Batch sizes (64, 32) for processing Event Hub messages.

### [0.0.30] 2023-10-18

	- Added /health and /heartbeat endpoints to validator API and transport service
 	- Configured batch size and prefetch on all functions for better performance
 	- Created and Sidecar function to save event to cosmos DB 
 	- Added CI/CD for Validator API and Replay API
	- Creating Open API documentation for our APIs -Replay, Validation API


### [0.0.29] - 2023/10/04
	
	- HL7Transform: Added extra fields on OBX, and handling OBX children of SPM, fixed some property name typos
	- Deployed our Validation API in DEV to fully support up to 100 msgs
		○ Designed and implemented a summary report for multiple messages
		○ Added Postman scripts for Happy Path tests
		○ Created first draft of our Open API documentation
	- Bug Fixes:
		○ Fixed couple of issues with PHLIP-FLU profile
	- Modified CI/CD Process to build artifact only once and deploy to TST with a single approval.
	- Added private end points to all missing resources in Azure DEV RG
	- Run performance tests to gauge CosmosDB and found some improvements due to high level of indexing done out-of-the-box 
	- Wrangling the replay API to deploy a first endpoint (/message_uuid) to DEV

### [0.0.28] - 2023-09-20
	- Migrated Structure validator to Elastic Plan in TST environment.
	- Updated Structure Validator to load profiles with lazy-load approach 
	- Changed all Functions to post events to Event Hub via annotation (@EventHubOutput)
	- Improved error handling on all functions to avoid losing messages
	- Added HTTP endpoint for HL7 Json for quick transformations
 	- Updated local-run tool with all changes from last and this sprint (cosmosDB, etc)
	
### [0.0.27] - 2023-09-06
	- All pipeline functions syncing events to Cosmos DB
	- All pipeline functions sending Events using annotation 
	- All pipeline functions migrated to Java 17 (Need DevOps support)
	- Clean up DEV and TST environments (remove Redis and deprecated functions)
	- Updated TST terraform scripts for current resources.
 	- Partitioning data from PHINMS and Mercury into year/month/day for Bulk load and daily load.

### [0.0.26] - 2023-08-23
	- Created an initial Validation API to expose our validation to States.
	- Created an initial Processing Monitor API to query status of a single message against our data lake.
	- Worked with Microsoft on Event Hubs Missing messages.
	- Recovered Lake of segments unit tests
	- Introduced Azure BOM on projects.
	- Finalize and test logback for fns-hl7-pipeline functions
	- Enhance DEX Metadata with "Unknown" source Metadata
	- Identify if PHINMS has extra metadata

### [0.0.25] - 2023-08-09
	- Delete all MMG related functions from the HL7 Pipeline.
	- Delete dependencies on Redis.
	- Added Event Code logic to Structure Validator.
	- Added handling of unexpected metadata in the incoming message.
	- Updated pipeline to align ELR and Case messages to a single pipeline.
	- Created Local Run tool allowing developers to run the HL7 pipeline on their local machines (Path: tools/local-run).
	- Created SVC Validator for API driven validation.
	- Started CosmosDB Writer, still in progress.
	- Enhanced unit tests across project libraries ( lib-dex-commons, lib-nist-validator, lib-bumblebee ) and HL7 functions.

### [0.0.24] - 2023-07-26
	- Completed Performance testing and fixed some bugs found during testing.
	- Fixed logging of Functions to show logs in Application Insights.
	- Completed the local-run tool to run the pipeline locally without dependency on Azure.
	- Completed Documentation for Programs.
	- Fixed svc-transport by adding a "ping" endpoint to make Azure happy.

### [0.0.23] - 2023-07-12
 	- Separated Unit test and Integration test for MMG Validator.
 	- Updated Structure Validator to use only Structure Error status for errors.
	- Parameterized container name on Receiver/Debatcher
	- Added Ping method to svc-cloud-transport for Azure requirements.


### [0.0.22] - 2023-06-28

	- Improved redactor to load different configuration for different message types/routes.
	- Value set errors on Structure Validator set to warning.
	- Updated summary metadata on all functions
	- Adding local_record_id to message_info metadata.
	- Fixed Original_file_name metadata 
	- Updated the upload_messages.py script to use multiple threads
	- Implementing CI/CD for continuous delivery to DEV/TST.

### [0.0.21] - 2023-06-14

	- Added Hepatitis 2.0 MMG
	- Added PHLIP FLU and PHLIP VPD Profiles
	- Bug fixes on PHIN Spec 3.x
	- Enhancements on Function for Performance purposes
	- Improved HL7 Transform to not populate fully empty objects
	- Unit test and coverage for MMG-Validator
	- Added redaction fields for PHLIP
	- Adding CI/CD process to deploy code changes after PRs.



### [0.0.20] - 2023-05-31
	- Created method to convert HL7 message into JSON, based on HL7-PET profiles.
	- Created AZ function to process hl7 messages into JSON model based on method above.
	- Updated HL7 Usage in Gen v1 MMG to "R" for all fields whose Cardinality is [1..1]
	- Removed ZLR segments from Covid19 ELR Nist Profiles
	- Improving logging statements of Receiver/Debatcher
	- Added Code coverage for Lake Of Segments
	- Improved Redis Proxy to use Connection pooling
	- Removed constraint for HD fields that requires a valid OID
	- Changed Unescaped delimiter errors from Error to Warning
	- Using Class level configuration for Fn to improve performance - read configuration only once on class lifecycle


### [0.0.19] - 2023-05-17
	- Handling Lab Templates for MMG-Based and MMG-in-SQL
	- Created Plan for Performance testing and ran some baselines in DEV env.
	- Added Unit test for Receiver, Redactor and Structure Validator.
	- Improvements on MMG-In-SQL configuration to reduce null values
	- Added logic to MMG-Based and MMG-In-SQL to handle only EPI Observation
	- Ignoring empty lines when ingesting HL7 message
	- Removed validation of NND value set against OBR-31 (event code)
	- Handling subcomponents on MMG-Based schema
	- Properly validating different OBR fields on GenV1

### [0.0.18] - 2023-05-03
 - Added support for new conditions:
	- Cryptosporidiosis
	- Cyclosporiasis
	- Cholera and Vibriosis
	- Campylobacteriosis
	- Salmonellosis
	- STEC
	- Shigellosis
	- S Typhi and S Paratyphi
	- FoodNet
	- Candida Auris
	- CP-CRE
 - Updated Receiver to be able to handle files in sub folders.
 - Updated MMG-Based and MMG-SQL to support new feature of the new conditions
	- Elements mapped more than in one block
	- Handling properly complex fields that are not populated
	- Added check for empty single collections.
 - Create sub tables for repeating elements in repeating groups for MMG-in-SQL
 - Added unit test and code coverage to Receiver/Debatcher and Redactor functions.
 - Added support for file upload to cloud-transport service and support for up to 10Mb files.
 - Renamed MSH-21 column names to be one based instead of zero based.
 - Removed rule that validates OBR-22 against OBR-7
 - Added validation for MMWR week.
 - Updated PHIN Spec profiles to match discrepancies finding testing against MVPS
 - Fixed fortify scan Critical and High issues.
 - Updated Readme files with latest changes and features
 - Updated all process names and process status on all functions to be consistent.



### [0.0.17] - 2023-04-19
  - Redacting ELR messages
  - Updated NIST Profiles to remove rules not present in MVPS 
  - Updated Legacy MMGs to not include subject/patient name type
  - Lake of Segments now processing ELR Messages
  - Added more MMG support - STD, congenital syphilis, Rubella, Congenital Rubella, Mumps, Malaria, Tuberculosis
  - Bug Fixes
     - Properly handling Repeating elements when creating inner lists in repeating table
     - Repeating elements of type ST not properly mapped
     - Changed repeat table to null instead of empty array in JSON output
     - Handling null repeating elements in repeating groups
     - Properly mapping primitive repeating elements


### [0.0.16] - 2023-04-06
  - Added config information to event metadata (configuration file names)
  - Added MUMPS, Pertussis, Tuberculosis, Babesiosis, Trichinellosis MMGs and Event Codes
  - Updates on PHIN Spec profiles
    - Replaced predicate statements with conformance statements for better uer messages.
    - Removed content validation of dates from IGAMT profiles into MMG validation.
  - Updated structure validator to validate HL7 delimiters
  - Creted tool to update IGAMT profile XML files with friendly error messages and deployed new updated profiles.   
  - Improved performance on MMG Validator by refactoring Redis lookups
  - Metadata bug fixes
    - Redacted did not update summary
    - Process metadata missing when exceptions were thrown on several functions.

  - Dev Ops
    -	Moved Redis to Premium in Dev and TST with automated backups
    -	Terraformed cloud service app and deployed in DEV, TST and STG (waiting on firewall for private end points)
    -	Started process to create ADF for AWS S3 Data migration.
    -	Created PhinMS Security Architecture diagram.
    -	Rerun Fortify Scans



### [0.0.15] - 2023-03-22
  - Set up STG environment and pipelines
  - Updated PHIN Specs with findings on discrepacies with MVPS validation
  - Shrotened the name of columns and repeating groups due to size limitations
  - Included extra fields to be redacted
  - Created redaction config for lab messages
  - Added config names to Processes for clarity on which configuration was used.
  - Several bug fixes.

### [0.0.14] - 2023-03-08
  - Set up TST environment and pipelines
  - Develop transport service to upload files with Metadata (pending deployment)
  - Designed and Develop Transport metadata to identify messages upon upload
  - Flaggged cardinality over as warnings
  - Added vocabulary check for OBR-31/GenV2 messages
  - Added COVID ELR profiles for structure validation
  - Bug fixes and Enhancements
    - Fixed NK1 Redaction paths
    - Routing unknwon messages properly
    - Fixed MMG-Based and MMG-In-SQL transform to support non-GenV2 messages
  
  

### [0.0.13] - 2023-02-22
  - Created new Function to Redact messages
  - Adding MMG for Varicella and TB Messages
  - Started JADs for incorporating ELR messages on the pipeline.
  - Created Lake of Segments transformations
  - Modified Validation rule for CARDINALITY_OVER to always be an WARNING (pipeline will pick first value if multiple are present)
  - Added EventHub metadata to Receiver/Debatcher function


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
