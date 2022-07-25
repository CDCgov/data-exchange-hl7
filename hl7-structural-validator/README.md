# HL7 Messages Structural Validation

This project is a wrapper around the NIST (https://www.nist.gov/) HL7 validator.

NIST jar's in the **/lib** folder are compiled from source code at https://github.com/usnistgov/v2-validation , to be compatible with Scala 2.12 (Spark).

Default profiles (src/main/resources/) are NNDSS profiles and are loaded and used if no other profiles are provided to the structure validator.

# Getting Started

Project is a Scala project and can run with sbt: https://www.scala-sbt.org/.

# Build and Test
```bash
$ sbt
> clean; compile; run
```
The assembly .jar is used for HL7 validation.
```bash
$ sbt 
> assembly 
```

# Example Usage
Add the assembly .jar as library to the project. The compiled assembly .jar is available in this repo / folder.

This example validates uses the custom build in profiles available in src/main/resources.

```scala
import cdc.xlr.structurevalidator._

val validator = StructureValidatorConc() // the concurrent (async) validator

// val validator = StructureValidatorSync() // the sync validator

validator.reportMap( hl7Message ) match {

    case Success(report) => println(report)
    case Failure(e) => println("error: " + e.getMessage() )

}

```

# Other

NIST repository:
https://hit-dev.nist.gov:9001/

However NIST available HL7 jar's at this time are not compatible with Scala 2.12 (Spark).
