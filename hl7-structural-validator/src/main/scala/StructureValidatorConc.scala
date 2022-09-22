package cdc.xlr.structurevalidator

import scala.concurrent.{Await, Future}
import scala.util.{Try}
import scala.concurrent.duration.{Duration}
import java.util.concurrent.{TimeUnit}

import hl7.v2.validation.HL7Validator
import gov.nist.validation.report.{Report}


class StructureValidatorAsync(profileLoader: ProfileLoader) extends StructureValidator {

    val profile = profileLoader.profile()
    val valueSets = profileLoader.valueSets()
    val confContext = profileLoader.conformanceContext()

    val validator = new HL7Validator(profile = profile, valueSetLibrary = valueSets, conformanceContext = confContext)

    // returns Future[Map[String, Any]]  the JSON validation report or the error
    def report(hl7Message: String): Try[Report] = {

      val reportFuture: Future[Report] = validator.validate(hl7Message, validator.profile.messages.keySet.head) 
      Try( Await.result(reportFuture, Duration(1, TimeUnit.SECONDS)) ) 
    } // .report

} // .StructureValidatorAsync


object StructureValidatorAsync {
    
  def apply(profileLoader: ProfileLoader) = new StructureValidatorAsync(profileLoader)
} // .StructureValidatorAsync
