#!/bin/bash
declare -a arr=("recdeb" "redacted" "struct")

if ! [[ "$1" =~ ^(dev|tst|stg|prd)$ ]]; then
	echo "You must pass the targe environment as paramter: dev, tst, stg or prd"
	return
fi

env=$1
hl7RG=ocio-ede-$env-moderate-hl7-rg


function_rootname=hl7-branch # must be the same as pom functionAppName property
base_name=az-fun-$function_rootname.zip

echo "Building Jar..."
mvn clean package -DskipTests=true -Paz-$env

echo "Zipping it:"

cd target/azure-functions/$function_rootname

zip -r ../../../$base_name *
cd ../../..

echo "Deploying Zip..."

for i in "${arr[@]}"
do
  echo "    Deploying $i on $1 Environment"
  az functionapp deployment source config-zip -g $hl7RG -n $function_rootname-$i-$env --src $base_name
done
