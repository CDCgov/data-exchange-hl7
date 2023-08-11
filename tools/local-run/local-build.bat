@echo off

call hello.bat
echo " BUILD LOCAL FUNCTIONS
echo "

rem set JAVA_HOME=C:\jdk\jdk-17.0.7+7
rem set PATH=C:\jdk\jdk-17.0.7+7\bin;%PATH%

set scriptpath=%~dp0

if "%1"=="" goto commons
if "%1"=="commons" goto commons
if "%1"=="debatcher" goto debatcher
if "%1"=="redactor" goto redactor
if "%1"=="structure" goto structure
if "%1"=="json-lake" goto json-lake
if "%1"=="lake-segs" goto lake-segs
if "%1"=="uploader" goto uploader
if "%1"=="summary" goto summary

echo " run as:local-build OR local-build  [commons|debatcher|redactor|structure|json-lake|lake-segs|uploader|summary]
echo "
echo " %RED%NOTHING TO BUILD%RESET%
echo "
goto bye

:commons
echo " building azure-commons...
cd azure-commons
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end


:debatcher
echo " building fn-debatcher-runner...
cd fn-debatcher-runner
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end

:redactor
echo " building fn-redactor-runner...
cd fn-redactor-runner
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end

:structure
echo " building fn-structure-runner...
cd fn-structure-runner
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end

:json-lake
echo " building fn-json-lake-runner...
cd fn-json-lake-runner
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end

:lake-segs
echo " building fn-lake-segs-runner...
cd fn-lake-segs-runner
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end

:uploader
echo " building storage-uploader...
cd storage-uploader
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd ..
if not "%1"=="" goto end

:summary
echo " building summary...
cd summary
call mvn --quiet compile
call mvn --quiet dependency:copy-dependencies
cd..

:end
echo "
echo " %GREEN%DONE%RESET%
echo "

:bye
cd %scriptpath%
call bye.bat
