#!/bin/bash

if [ -z "$1" ]
then
  echo "Make sure you pass month/day as param. ex.: 03/25"
  return
fi

echo "@@@"
echo "@@@ Counting Upload API"
echo "@@@"

echo "Counting HL7"
az storage blob list -c "aims-celr-hl7" --account-key="${UPLOAD_ACCT_KEY}" --account-name="ocioededataexchangestg" --query "length(@)" --prefix "2024/$1" --num-results 2147483647


echo "Counting CSV"
az storage blob list -c "aims-celr-csv" --account-key="${UPLOAD_ACCT_KEY}" --account-name="ocioededataexchangestg" --query "length(@)" --prefix "2024/$1" --num-results 2147483647


echo "@@@"
echo "@@@ Counting routeingress"
echo "@@@"

echo "Counting HL7"
 az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "aims-celr-hl7/2024/$1" --num-results 2147483647

echo "Counting csv"
 az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "aims-celr-csv/2024/$1" --num-results 2147483647

declare -a folders=("hl7_out_recdeb" "hl7_out_redacted" "hl7_out_validation_report" "hl7_out_json" "hl7_out_lake_seg")

echo "@@@"
echo "@@@ Counting hl7 outputs"
echo "@@@"

for i in "${folders[@]}"
do
  echo "Counting $i/2024/$1"
  az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "$i/2024/$1" --num-results 2147483647

done

declare -a folders=("hl7_out_recdeb" "hl7_out_redacted" "hl7_validation_report" "hl7_out_json" "hl7_lake_seg")

echo "@@@"
echo "@@@ Counting ezdx"
echo "@@@"

for i in "${folders[@]}"
do
  echo "Counting $i/2024/$1"
  count=$(az storage blob list -c "dex" --account-key "${EZDX_ACCT_KEY}" --account-name "stezdxstg" --query "length(@)"  --prefix "$i/2024/$1" --num-results 2147483647)
  echo $(($count-25)) # Remove folder count - make sure there is one folder for each hour of the day 00 to 23, 24 total + parent folder = 25
done

echo "@@@"
echo "@@@ Checking Dead letter"
echo "@@@"
declare -a folders=("hl7_out_recdeb" "hl7_out_redacted" "hl7_out_validation_report" "hl7_out_json" "hl7_out_lake_seg")
for i in "${folders[@]}"
do
  echo "Counting $i/2024/$1"
  az storage blob list -c "route-deadletter" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "$i/2024/$1" --num-results 2147483647
done

echo "@@@"
echo "@@@ Checking Processing Status"
echo "@@@"
date=$(echo $1 | tr -d /)

#echo "HL7 Reports"
hl7PS=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts?data_stream_id=aims-celr&data_stream_route=hl7&date_start=2024${date}T000000Z&date_end=2024${date}T235900Z&page_size=1&page_number=1" | jq -r '.summary.total_items')

#echo "CSV Reports"
csvPS=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts?data_stream_id=aims-celr&data_stream_route=csv&date_start=2024${date}T000000Z&date_end=2024${date}T235900Z&page_size=1&page_number=1" | jq -r '.summary.total_items')

#echo Invalid Message Reports:
invalidPS=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts/hl7/invalidStructureValidation?data_stream_id=aims-celr&data_stream_route=hl7&date_start=2024${date}T000000Z&date_end=2024${date}T235900Z" | jq -r '.counts' )

echo "HL7 Reports: $hl7PS"
echo "CSV Reports: $csvPS"
echo "INV Reports: $invalidPS"