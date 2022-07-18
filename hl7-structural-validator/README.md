# HL7 Messages Structural Validation

This project is a wrapper around the NIST (https://www.nist.gov/) HL7 validator.

NIST jar's in the **/lib** folder are compiled from source code at https://github.com/usnistgov/v2-validation , to be compatible with Scala 2.12 (Spark).

Default profiles are NNDSS profiles and loaded at start-up but other profiles could be build in or dynamic loaded as enhancement.

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
Add (assembly) .jar as library to the project. The compiled assembly .jar is available in this repo folder.

```scala
import cdc.xlr.structurevalidator._

// Async Validator

val validator = StructureValidator()

val result = validator.validate(testMsg) onComplete {
    case Success(report) => //println(report)
    case Failure(err) => println(err.getMessage)
} // .result
```

```scala
import cdc.xlr.structurevalidator._

//Sync Validator

val validator = StructureValidatorSync()
val result = validator.validate(testMsg)

```

# Other

NIST repository:
https://hit-dev.nist.gov:9001/

However NIST available HL7 jar's at this time are not compatible with Scala 2.12 (Spark).
