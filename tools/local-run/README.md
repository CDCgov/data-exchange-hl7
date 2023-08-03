### LOCAL EVENT HUBS SIMULATOR FOR FNS-HL7-PIPELINE

The code has been tested with kotlin 1.8.0, jdk-11.0.18+10 and jdk-17.0.7+7

Prerequisites:  
TODO

All following instructions assume that "Command Prompt" is open and  
the current folder is set to:  
C:\...\data-exchange-hl7\tools\local-run

Initial Setup, run the following batch files in sequence:
- hub-config  
  creates event-hubs and storage folder
- fns-build    
  compiles fns-hl7-pipeline functions
- local-build   
  compiles local functions
- local-run    
  runs the local event hub simulator

After running local-run batch file you will see that there are no messages to process.

The simulator consumes messages from the storage/hl7ingress folder.   
There are two ways to get messages in the hl7ingress:
1. Use fn-storage-uploader azure function
    - cd fn-storage-uploader
    - Initial compile only:  
      mvn package -DskipTests
    - mvn azure-functions:run  
      After that you can use Postman to upload messages
2. Use storage-uploader project
    - cd storage-uploader
    - Initial compile only:  
      mvn package -DskipTests
    - Edit run.bat to set environment variables to point to fns-hl7-pipeline unit test folders or other folders with hl7 messages  
      There are REM comments in the batch file that describe what is needed for CASE or ELR
    - run  
      It will move the files to hl7ingress and start the simulator

After the simulator runs you can open view_hubs.html to see the hubs messages.  
Message content can be examined with view_msg.html

You can compile\run all or a single simulator project.  
For example: local-run debatcher will run the debatcher simulator.  
Type fns-build ? or local-build ? or local-run for options.

To clear the hubs and storage:
- from local- run folder:
    - del *.txt /s
    - del *.properties /s  

