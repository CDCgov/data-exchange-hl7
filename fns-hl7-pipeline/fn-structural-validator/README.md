# HL7 Validator function


## Example install local dependency 

- .jar available in /hl7-structural-validator

```
mvn install:install-file \
    -Dfile=cdc.xlr.hl7.structurevalidator-assembly-0.4.4.jar \
    -DgroupId=cdc.xlr.structurevalidator \
    -DartifactId=cdc-xlr-hl7-structurevalidator \
    -Dversion=0.4.4 \
    -Dpackaging=jar \
    -DgeneratePom=true
```