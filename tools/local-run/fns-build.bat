@echo off

call hello.bat
echo " BUILD FNS-HL7-PIPELINE
echo "

rem set JAVA_HOME=C:\jdk\jdk-17.0.7+7
rem set PATH=C:\jdk\jdk-17.0.7+7\bin;%PATH%

set scriptpath=%~dp0

set FNS_HOME=..\..\fns-hl7-pipeline

if "%1"=="" goto debatcher
if "%1"=="debatcher" goto debatcher
if "%1"=="redactor" goto redactor
if "%1"=="structure" goto structure
if "%1"=="json-lake" goto json-lake
if "%1"=="lake-segs" goto lake-segs

echo " run as:fns-build OR fns-build  [debatcher|redactor|structure|json-lake|lake-segs]
echo "
echo " %RED%NOTHING TO BUILD%RESET%
echo "
goto bye

:debatcher
echo " fn-receiver-debatcher...
cd %FNS_HOME%/fn-receiver-debatcher
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
if not "%1"=="" goto end

:redactor
echo " fn-redactor...
cd %FNS_HOME%/fn-redactor
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
if not "%1"=="" goto end

:structure
echo " fn-structure-validator...
cd %FNS_HOME%/fn-structure-validator
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
if not "%1"=="" goto end

:json-lake
echo " fn-hl7-json-lake...
cd %FNS_HOME%/fn-hl7-json-lake
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
if not "%1"=="" goto end

:lake-segs
echo " fn-lake-segs-transformer...
cd %FNS_HOME%/fn-lake-segs-transformer
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
if not "%1"=="" goto end

:end
echo "
echo " %GREEN%DONE%RESET%
echo "

:bye
cd %scriptpath%
call bye.bat
