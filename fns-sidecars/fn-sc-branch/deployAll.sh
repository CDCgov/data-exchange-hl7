
declare -a arr=("recdeb" "redacted" "struct")

for i in "${arr[@]}"
do
  echo "Deploying $i"
  mvn clean package -DskipTests -Paz-$1 -DFUNC="$i"
  mvn azure-functions:deploy -DFUNC="$i"
done
