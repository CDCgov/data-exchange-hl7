setlocal

set EventHubConnectionString=%EVHUB_HOME%/event-hubs
set EventHubReceiveName=hl7-recdeb-ok
set EventHubSendErrsName=hl7-redacted-err
set EventHubSendOkName=hl7-redacted-ok
set ContainerPath=%EVHUB_HOME%/storage/hl7-events/hl7-redacted

java -classpath %EVHUB_HOME%/azure-commons/target/classes;%FNS_HOME%/fn-redactor/target/dependency/*;%FNS_HOME%/fn-redactor/target/classes;target/dependency/*;target/classes RunClass
rem java -classpath %EVHUB_HOME%/azure-commons/target/azure-commons-1.0-SNAPSHOT.jar;%FNS_HOME%/fn-redactor/target/azure-functions/ocio-ede-dev-hl7-Redactor/lib/*;%FNS_HOME%/fn-redactor/target/redactor-1.0.0-SNAPSHOT.jar;target/dependency/*;target/fn-redactor-runner-1.0-SNAPSHOT.jar  RunClass