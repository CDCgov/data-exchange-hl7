# HL7 Messages Structural Validation

This project is a wrapper around the NIST (https://www.nist.gov/) HL7 validator.

NIST jar's in the **/lib** folder are compiled from NIST v2-validation repoo source code available at https://github.com/usnistgov/v2-validation

Default profiles (src/main/resources/) are NNDSS profiles and are loaded and used if no other profiles are provided to the structure validator.

# Getting Started

Project is a Scala project and can run with sbt: https://www.scala-sbt.org/.

### Build and Run
```bash
$ sbt
> clean; compile; run
```
### Test
```bash
$ sbt
> test
```
### Package
```bash
$ sbt
> test
```
### Package (Assembly) - The assembly .jar is used for HL7 validation.
```bash
$ sbt 
> assembly 
```

# Example Usage
Add the assembly .jar as library to the project. The compiled assembly .jar is available in this repo / folder.

This example validates using the profiles available in src/main/resources.

```scala
import cdc.xlr.structurevalidator._

// 2 validator's ara available: async (concurrent) and sync (synchronous) 

val validator = StructureValidatorAsync(ProfileLoaderLocal(PROFILES_PHIN_SPEC_3_1))   // the async validator 

validator.reportMap( hl7Message ) match {

    case Success(report) => println(report)
    case Failure(e) => println("error: " + e.getMessage() )

}

// other validator methods available: 

// validator.reportJSON -> returns a validation report in JSON format

// validator.report -> returns a NIST report (gov.nist.validation.report.{Entry, Report})

```

