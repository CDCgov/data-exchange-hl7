# Function HL7 Messages Structural Validation

Cloud function for the HL7 Messages Structural Validation. 

### Library

The __assembly__ .jar created from: [hl7-structural-validator](https://github.com/CDCgov/data-exchange-hl7/tree/master/hl7-structural-validator) is used as library.

Example: 
- Create assembly .jar
- Copy assembly .jar to /artifacts
- Install using Maven:

```scala
mvn install:install-file \
    -Dfile=artifacts/cdc.xlr.hl7.structurevalidator-assembly-0.3.1.jar \
    -DgroupId=cdc.xlr.structurevalidator \
    -DartifactId=cdc-xlr-hl7-structurevalidator \
    -Dversion=0.3.1 \
    -Dpackaging=jar \
    -DgeneratePom=true
```


