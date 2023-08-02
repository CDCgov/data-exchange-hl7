@echo off

setlocal

rem set JAVA_HOME=C:\jdk\jdk-17.0.7+7
rem set PATH=C:\jdk\jdk-17.0.7+7\bin;%PATH%

set FNS_HOME=..\..\..\fns-hl7-pipeline
set EVHUB_HOME=..\

rem ELR
set MSG_SOURCE_FOLDER=%FNS_HOME%/fn-structure-validator/src/test/resources/covidELR
set MSG_OUTPUT_FOLDER=%EVHUB_HOME%/storage/hl7ingress
set MSG_TYPE=ELR
set ROUTE=COVID19_ELR

rem CASE
rem set MSG_SOURCE_FOLDER=%EVHUB_HOME%/fn-artefacts/case
rem MSG_OUTPUT_FOLDER=%EVHUB_HOME%/storage/hl7ingress
rem set MSG_TYPE=CASE

java -classpath target/dependency/*;target/classes RunClass
rem java -classpath target/dependency/*;target/storage-uploader-1.0-SNAPSHOT.jar  RunClass

cd ..
call local-run