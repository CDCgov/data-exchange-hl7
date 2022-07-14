# Function HL7 Messages Structural Validation

Cloud function for the HL7 Messages Structural Validation: [hl7-structural-validator](https://github.com/CDCgov/data-exchange-hl7/tree/master/hl7-structural-validator). 

The assembly .jar is used as library.


### Install hl7-structural-validator assembly .jar

```scala
mvn install:install-file \
    -Dfile=artifacts/cdc.xlr.hl7.structurevalidator-assembly-0.3.1.jar \
    -DgroupId=cdc.xlr.structurevalidator \
    -DartifactId=cdc-xlr-hl7-structurevalidator \
    -Dversion=0.3.1 \
    -Dpackaging=jar \
    -DgeneratePom=true
```


