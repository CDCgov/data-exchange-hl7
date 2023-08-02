LOCAL EVENT HUBS SIMULATOR FOR FNS-HL7-PIPELINE 

The code has been tested with jdk-11.0.18+10 and jdk-17.0.7+7

Preconditions:
TODO

Initial Setup:    
    1)  Open “Command Prompt”. 
           If you read this, you already have cloned the develop branch of the DEX project or checkout eph0-local-run branch.
           Navigate to data-exchange-hl7\tools\local-run folder
    2)  Run hub-setup.bat. It will setup event-hubs and storage folders
    3)  Run fns-build.bat It will compile fns-hl7-pipeline functions
    4)  Run local-build.bat. It will compile simulator local projects
    5)  Run local-run.bat it will run the pipeline

After 5) you will see that there are no messages to process.

The simulator consumes messages from storage/hl7ingress
There are two ways to get messages in the hl7ingress:
    1)  Use fn-storage-uploader azure function
          cd fn-storage-uploader
          Initial compile only: mvn package -DskipTests
          mvn azure-functions:run
          After that you can use Postman to upload messages
    2)  Use storage-uploader project
          cd storage-uploader
          Initial compile only: mvn package -DskipTests
          Edit run.bat to set env. variables to point to fns-hl7-pipeline unit test folders or other folders with hl7 messages
          There are REM comments in the batch file that describe what is needed for CASE or ELR
          run.bat
          It will move the files to hl7ingress and start the simulator

After the simulator runs you can open view_hubs.html to see the hubs messages.
Message content can be examined with view_msg.html

You can compile\run all or a single simulator project.
For example: local-run debatcher will run the debatcher simulator.
Type fns-build ? or local-build ? or local-run for options.

To clear the hubs and storage: del *.txt /s  from the local-run folder
