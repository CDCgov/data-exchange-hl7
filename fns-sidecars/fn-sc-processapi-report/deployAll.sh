
declare -a arr=("recdeb" "redacted" "struct" "json-lake" "lake-seg")

if ! [[ "$1" =~ ^(dev|tst|stg)$ ]]; then
	echo "You must pass the targe environment as paramter: dev, tst or stg"
	return
fi

for i in "${arr[@]}"
do
  echo "@@@"
  echo "@@@ Deploying $i on $1 Environment"
  echo "@@@"
  mvn clean package -DskipTests -Paz-"$1" -DFUNC="$i"
  mvn azure-functions:deploy -Paz-"$1" -DFUNC="$i"
done
