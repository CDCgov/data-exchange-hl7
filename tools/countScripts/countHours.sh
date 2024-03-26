#!/bin/bash

if [ -z "$1" ]
then
  echo "Make sure you pass month/day as param. ex.: 03/25"
  return
fi

declare -a hours=("00" "01" "02" "03" "04" "05" "06" "07" "08" "09" "10" "11" "12" "13" "14" "15" "16" "17" "18" "19" "20" "21" "22" "23")

echo "@@@"
echo "@@@ Counting routeingress"
echo "@@@"

echo "Counting HL7"
hl7=$(az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "aims-celr-hl7/2024/$1" --num-results 2147483647)

echo "hl7 -> $hl7"
echo "Counting csv"
csv=$(az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "aims-celr-csv/2024/$1" --num-results 2147483647)
echo "csv -> $csv"
declare -a  foldersHL7=("hl7_out_recdeb" "hl7_out_redacted" "hl7_out_validation_report" "hl7_out_json" "hl7_out_lake_seg")
declare -a foldersEZDX=("hl7_out_recdeb" "hl7_out_redacted" "hl7_validation_report" "hl7_out_json" "hl7_lake_seg")


echo "@@@"
echo "@@@ Counting hl7 outputs"
echo "@@@"

for h in "${hours[@]}"
do	
	declare -a hl7F=("" "" "" "" "")
	idx=0

	for i in "${foldersHL7[@]}"
	do
		#  echo "Counting $i/2024/$1/$h"
		  hl7F[$idx]=$(az storage blob list -c "routeingress" --account-key "${ROUTE_ACCT_KEY}" --account-name "ocioederoutingdatasastg" --query "length(@)"  --prefix "$i/2024/$1/$h/" --num-results 2147483647)
		idx=$idx+1
	done

	declare -a ezdxF=("" "" "" "" "")
	idx=0
	
	for i in "${foldersEZDX[@]}"
	do
		  #echo "Counting $i/2024/$1/$h"
		  ezdxF[$idx]=$(az storage blob list -c "dex" --account-key "${EZDX_ACCT_KEY}" --account-name "stezdxstg" --query "length(@)"  --prefix "$i/2024/$1/$h/" --num-results 2147483647)
		idx=$idx+1
	done
	echo "$h -> ${hl7F[*]} ==> ${ezdxF[*]}"
done


