setlocal

set EventHubConnectionString=%EVHUB_HOME%/event-hubs
set EventHubConsumerGroupCASE=ocio-ede-dev-hl7-lake-segments-case-cg-001
set EventHubConsumerGroupELR=ocio-ede-dev-hl7-lake-segments-elr-cg-001
set EventHubReceiveNameCASE=hl7-structure-ok
set EventHubSendOkName=hl7-lake-segments-ok
set EventHubSendErrsName=hl7-lake-segments-err
set ContainerPath=%EVHUB_HOME%/storage/hl7-events/hl7-lake-segments

java -classpath %EVHUB_HOME%/azure-commons/target/classes;%FNS_HOME%/fn-lake-segs-transformer/target/dependency/*;%FNS_HOME%/fn-lake-segs-transformer/target/classes;target/dependency/*;target/classes  RunClass
rem java -classpath %EVHUB_HOME%/azure-commons/target/azure-commons-1.0-SNAPSHOT.jar;%FNS_HOME%/fn-lake-segs-transformer/target/azure-functions/ocio-ede-dev-hl7-lake-segments-transformer/lib/*;%FNS_HOME%/fn-lake-segs-transformer/target/lake-segs-transfomer-1.0.0-SNAPSHOT.jar;target/dependency/*;target/fn-lake-segs-runner-1.0-SNAPSHOT.jar  RunClass