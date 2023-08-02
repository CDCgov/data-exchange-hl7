@echo off

call hello.bat
echo " EVENT HUB LOCAL RUN
echo "

rem set JAVA_HOME=C:\jdk\jdk-17.0.7+7
rem set PATH=C:\jdk\jdk-17.0.7+7\bin;%PATH%

set FNS_HOME=..\..\..\fns-hl7-pipeline
set EVHUB_HOME=%~dp0
set LOG-LEVEL=OFF

set scriptpath=%~dp0

if "%1"=="" goto debatcher
if "%1"=="debatcher" goto debatcher
if "%1"=="redactor" goto redactor
if "%1"=="structure" goto structure
if "%1"=="json-lake" goto json-lake
if "%1"=="lake-segs" goto lake-segs

echo " run as: local-run OR local-run [debatcher|redactor|structure|json-lake|lake-segs]
echo "
echo " %RED%NOTHING TO RUN%RESET%
echo "
goto bye

:debatcher
echo " %CYAN%fn-receiver-debatcher%RESET%
cd fn-debatcher-runner
call run
cd ..
if not "%1"=="" goto end

:redactor
echo "  %CYAN%fn-redactor%RESET%
cd fn-redactor-runner
call run
cd ..
if not "%1"=="" goto end

:structure
echo "  %CYAN%fn-structure-validator%RESET%
cd fn-structure-runner
call run
cd ..
if not "%1"=="" goto end

:json-lake
echo "  %CYAN%fn-hl7-json-lake%RESET%
cd fn-json-lake-runner
call run
cd ..
if not "%1"=="" goto end

:lake-segs
echo "  %CYAN%fn-lake-segs-transformer%RESET%
cd fn-lake-segs-runner
call run
cd ..

:end
cd summary
call run

echo "
echo " %GREEN%DONE%RESET%
echo "

:bye
cd %scriptpath%
call bye.bat
