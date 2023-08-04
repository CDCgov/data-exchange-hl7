@echo off

call hello.bat

echo " EVENT HUBS CONFIG
echo "

set scriptpath=%~dp0

if not exist "event-hubs" (
  mkdir event-hubs
)
cd event-hubs
if not exist "hl7-json-lake-err" (
  mkdir hl7-json-lake-err
)

if not exist "hl7-json-lake-ok" (
  mkdir  hl7-json-lake-ok
)

if not exist "hl7-lake-segments-err" (
  mkdir  hl7-lake-segments-err
)

if not exist "hl7-lake-segments-ok" (
  mkdir  hl7-lake-segments-ok
)

if not exist "hl7-recdeb-err" (
  mkdir  hl7-recdeb-err
)

if not exist "hl7-recdeb-ok" (
  mkdir  hl7-recdeb-ok
)

if not exist "hl7-redacted-err" (
  mkdir  hl7-redacted-err
)

if not exist "hl7-redacted-ok" (
  mkdir  hl7-redacted-ok
)

if not exist "hl7-structure-err" (
  mkdir  hl7-structure-err
)

if not exist "hl7-structure-ok" (
  mkdir  hl7-structure-ok
)

cd..

if not exist "storage" (
  mkdir storage
)

if not exist "storage\hl7ingress" (
  mkdir storage\hl7ingress
)

cd %scriptpath%
set scriptpath=

echo " %GREEN%DONE[0m
echo "
call bye.bat


