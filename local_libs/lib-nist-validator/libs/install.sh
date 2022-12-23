export NIST_VERSION=1.5.5

mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=xml-util-2.1.0.jar -DgroupId=gov.nist -DartifactId=xml-util -Dversion=2.1.0 -Dpackaging=jar -DpomFile=xml-util-2.1.0.pom
mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=validation-report-1.1.0.jar -DgroupId=com.github.hl7-tools -DartifactId=validation-report -Dversion=1.1.0 -Dpackaging=jar -DpomFile=validation-report-1.1.0.pom


mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=hl7-v2-profile-$NIST_VERSION.jar -DgroupId=gov.nist -DartifactId=hl7-v2-profile -Dversion=$NIST_VERSION -Dpackaging=jar -DpomFile=hl7-v2-profile-$NIST_VERSION.pom
mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=hl7-v2-parser-$NIST_VERSION.jar -DgroupId=gov.nist -DartifactId=hl7-v2-parser -Dversion=$NIST_VERSION -Dpackaging=jar -DpomFile=hl7-v2-parser-$NIST_VERSION.pom
mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=hl7-v2-validation-$NIST_VERSION.jar -DgroupId=gov.nist -DartifactId=hl7-v2-validation -Dversion=$NIST_VERSION -Dpackaging=jar -DpomFile=hl7-v2-validation-$NIST_VERSION.pom

