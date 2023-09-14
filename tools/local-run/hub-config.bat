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

if not exist "storage\hl7-events" (
  mkdir storage\hl7-events
)

if not exist "storage\hl7-events\hl7-recdeb" (
  mkdir storage\hl7-events\hl7-recdeb
)

if not exist "storage\hl7-events\hl7-redacted" (
  mkdir storage\hl7-events\hl7-redacted
)

if not exist "storage\hl7-events\hl7-structure" (
  mkdir storage\hl7-events\hl7-structure
)

if not exist "storage\hl7-events\hl7-json-lake" (
  mkdir storage\hl7-events\hl7-json-lake
)

if not exist "storage\hl7-events\hl7-lake-segments" (
  mkdir storage\hl7-events\hl7-lake-segments
)


cd %scriptpath%
set scriptpath=

echo " %GREEN%DONE[0m
echo "
call bye.bat


