package cdc.xlr.structurevalidator

import scala.util.{Failure, Success, Try}

import hl7.v2.validation.SyncHL7Validator
import gov.nist.validation.report.{Report}


class StructureValidatorSync(profileLoader: ProfileLoader) extends StructureValidator {

    val profile = profileLoader.profile()
    val valueSets = profileLoader.valueSets()
    val confContext = profileLoader.conformanceContext()

    val validator = new SyncHL7Validator(profile = profile, valueSetLibrary = valueSets, conformanceContext = confContext)

    def report(hl7Message: String): Try[Report] = {
      
      try {

        val report = validator.check(hl7Message, validator.profile.messages.keySet.head)
        Success(report) 

      } catch {

        case e: Throwable => Failure(e)

      } // .try-catch
      
    } // .report

} // .StructureValidatorSync

object StructureValidatorSync {

  def apply() = new StructureValidatorSync(new ProfileLoaderLocal)
  def apply(profileLoader: ProfileLoader) = new StructureValidatorSync(profileLoader)
} // .StructureValidatorSync


