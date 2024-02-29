sed -i '/auth>/d' ./pom.xml
sed -i '/<type>service_principal/d' ./pom.xml
sed -i '/<serverId>azure-service-principal/d' ./pom.xml

