setlocal

set BlobIngestConnectionString=%EVHUB_HOME%/storage
set BlobIngestContainerName=hl7ingress
set EventHubConnectionString=%EVHUB_HOME%/event-hubs
set EventHubConsumerGroup=fn-receiver-debatcher-consumer-group
set EventHubReceiveName=hl7-file-dropped
set EventHubSendErrsName=hl7-recdeb-err
set EventHubSendOkName=hl7-recdeb-ok
set ContainerPath=%EVHUB_HOME%/storage/hl7-events/hl7-recdeb

java -classpath %EVHUB_HOME%/azure-commons/target/classes;%FNS_HOME%/fn-receiver-debatcher/target/dependency/*;%FNS_HOME%/fn-receiver-debatcher/target/classes;target/dependency/*;target/classes  RunClass
rem java -classpath %EVHUB_HOME%/azure-commons/target/azure-commons-1.0-SNAPSHOT.jar;%FNS_HOME%/fn-receiver-debatcher/target/azure-functions/ocio-ede-dev-hl7-receiver-debatcher/lib/*;%FNS_HOME%/fn-receiver-debatcher/target/receiver-debatcher-1.0.0-SNAPSHOT.jar;target/dependency/*;target/fn-debatcher-runner-1.0-SNAPSHOT.jar  RunClass