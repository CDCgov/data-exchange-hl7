
DATA_STREAM="aims-celr"
HL7_ROUTE="hl7"
CSV_ROUTE="csv"

if [ -z "$1" ]
then
  echo "Make sure you pass month/day as param. ex.: 03/25"
  return
fi

echo "@@@"
echo "@@@ Counting Upload API"
echo "@@@"

echo "Counting HL7"
az storage blob list -c "$DATA_STREAM-$HL7_ROUTE" --account-key="${UPLOAD_ACCT_KEY}" --account-name="ocioededataexchangestg" --query "length(@)" --prefi "2024/$1" --num-results 2147483647


echo "Counting CSV"
az storage blob list -c "$DATA_STREAM-$CSV_ROUTE" --account-key="${UPLOAD_ACCT_KEY}" --account-name="ocioededataexchangestg" --query "length(@)" --prefi "2024/$1" --num-results 2147483647


echo "@@@"
echo "@@@ Counting routeingress"
echo "@@@"

echo "Counting HL7"
 az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "$DATA_STREAM-$HL7_ROUTE/2024/$1" --num-results 2147483647

echo "Counting csv"
 az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "$DATA_STREAM-$CSV_ROUTE/2024/$1" --num-results 2147483647

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

count=$(az storage blob list -c "dex" --account-key "${EZDX_ACCT_KEY}" --account-name "stezdxstg" --query "length(@)"  --prefix "$DATA_STREAM-$CSV_ROUTE/2024/$1" --num-results 2147483647)
echo "CSV: $(($count-1))" # Remove root folder counted on query above.

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
hl7PS=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts?data_stream_id=$DATA_STREAM&data_stream_route=$HL7_ROUTE&date_start=2024${date}T000000Z&date_end=2024${date}T235959Z&page_size=1&page_number=1" | jq -r '.summary.total_items')

hl7Complete=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts/uploadStats?data_stream_id=$DATA_STREAM&data_stream_route=$HL7_ROUTE&date_start=2024${date}T000000Z&date_end=2024${date}T235959Z&page_size=1&page_number=1" | jq -r '.completed_upload_count')
#echo "CSV Reports"
csvPS=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts?data_stream_id=$DATA_STREAM&data_stream_route=$CSV_ROUTE&date_start=2024${date}T000000Z&date_end=2024${date}T235959Z&page_size=1&page_number=1" | jq -r '.summary.total_items')

#echo Invalid Message Reports:
invalidPS=$(curl -s "https://ocio-ede-stg-pstatus-api.azurewebsites.net/api/report/counts/hl7/invalidStructureValidation?data_stream_id=$DATA_STREAM&data_stream_route=$HL7_ROUTE&date_start=2024${date}T000000Z&date_end=2024${date}T235959Z" | jq -r '.counts' )

echo "HL7 Attempts: $hl7PS"
echo "HL7 Complete: $hl7Complete"
echo "CSV Reports: $csvPS"
echo "Invalid Message Reports: $invalidPS"
