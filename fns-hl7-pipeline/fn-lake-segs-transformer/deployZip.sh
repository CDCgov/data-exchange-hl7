#!/bin/bash
if ! [[ "$1" =~ ^(dev|tst|stg|prd)$ ]]; then
	echo "You must pass the targe environment as paramter: dev, tst, stg or prd"
	return
fi
env=$1
hl7RG=ocio-ede-$env-moderate-hl7-rg

base_name=az-fun-hl7-lake-segments-transformer.zip
function=ocio-ede-$env-hl7-lake-segments-transformer

echo "Building Jar..."
mvn clean package -DskipTests=true -Paz-$env

echo "Zipping it:"

cd target/azure-functions/$function

zip -r ../../../$base_name *
cd ../../..

echo "Deploying Zip..."

az functionapp deployment source config-zip -g $hl7RG -n $function --src $base_name