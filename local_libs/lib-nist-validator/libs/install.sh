
mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=hl7-v2-profile-1.5.1.jar -DgroupId=gov.nist -DartifactId=hl7-v2-profile -Dversion=1.5.1 -Dpackaging=jar -DpomFile=hl7-v2-profile-1.5.1.pom

mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=hl7-v2-parser-1.5.1.jar -DgroupId=gov.nist -DartifactId=hl7-v2-parser -Dversion=1.5.1 -Dpackaging=jar -DpomFile=hl7-v2-parser-1.5.1.pom


mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=hl7-v2-validation-1.5.1.jar -DgroupId=gov.nist -DartifactId=hl7-v2-validation -Dversion=1.5.1 -Dpackaging=jar -DpomFile=hl7-v2-validation-1.5.1.pom

mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile= -DgroupId=gov.nist -DartifactId=xml-util -Dversion=1.0.1-SNAPSHOT -Dpackaging=jar
