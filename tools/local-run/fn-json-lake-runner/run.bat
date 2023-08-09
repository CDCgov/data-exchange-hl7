setlocal

set EventHubConnectionString=%EVHUB_HOME%/event-hubs
set EventHubConsumerGroupCASE=ocio-ede-dev-hl7-json-lake-case-cg-001
set EventHubReceiveNameCASE=hl7-structure-ok
set EventHubSendOkName=hl7-json-lake-ok
set EventHubSendErrsName=hl7-json-lake-err

java -classpath %EVHUB_HOME%/azure-commons/target/classes;%FNS_HOME%/fn-hl7-json-lake/target/dependency/*;%FNS_HOME%/fn-hl7-json-lake/target/classes;target/dependency/*;target/classes  RunClass
rem java -classpath %EVHUB_HOME%/azure-commons/target/azure-commons-1.0-SNAPSHOT.jar;%FNS_HOME%/fn-hl7-json-lake/target/azure-functions/ocio-ede-dev-hl7-json-lake-fn/lib/*;%FNS_HOME%/fn-hl7-json-lake/target/hl7-json-lake-fn-1.0.0-SNAPSHOT.jar;target/dependency/*;target/fn-json-lake-runner-1.0-SNAPSHOT.jar  RunClass