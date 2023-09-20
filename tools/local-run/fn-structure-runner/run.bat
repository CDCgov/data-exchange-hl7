setlocal

set EventHubConnectionString=%EVHUB_HOME%/event-hubs
set EventHubReceiveName=hl7-redacted-ok
set EventHubSendErrsName=hl7-structure-err
set EventHubSendOkName=hl7-structure-ok
set ContainerPath=%EVHUB_HOME%/storage/hl7-events/hl7-structure

java -classpath %EVHUB_HOME%/azure-commons/target/classes;%FNS_HOME%/fn-structure-validator/target/dependency/*;%FNS_HOME%/fn-structure-validator/target/classes;target/dependency/*;target/classes RunClass
rem java -classpath %EVHUB_HOME%/azure-commons/target/azure-commons-1.0-SNAPSHOT.jar;%FNS_HOME%/fn-structure-validator/target/azure-functions/ocio-ede-dev-hl7-structure-validator/lib/*;%FNS_HOME%/fn-structure-validator/target/structure-validator-1.0.0-SNAPSHOT.jar;target/dependency/*;target/fn-structure-runner-1.0-SNAPSHOT.jar  RunClass