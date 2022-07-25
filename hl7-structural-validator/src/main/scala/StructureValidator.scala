package cdc.xlr.structurevalidator

import scala.util.{Try}

import gov.nist.validation.report.{Entry, Report}
import scala.util.Failure
import scala.util.Success

 import scala.collection.JavaConverters._

trait StructureValidator {

  def report(hl7Message: String): Try[Report]

  def reportMap(hl7Message: String): Try[Map[String, List[Entry]]] = {

    report(hl7Message) match {

      case Failure(exception) => Failure(exception)

      case Success(value) => {

        val reportMap = value.getEntries().asScala.mapValues(_.asScala.toList)

        val errorClassification = "Error"
        val warningClassification = "Warning"

        Success( Map(
          // report details 
          "structureErrors" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(errorClassification)),
          "structureWarnings" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(warningClassification)),

          "valueSetErrors" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(errorClassification)),
          "valueSetWarnings" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(warningClassification)),

          "contentErrors" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(errorClassification)),
          "contentWarnings" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(warningClassification)),
        )) // .Map

      }// .Success
    } // .report 

  } // .reportMap 


  // def reportJSON: String

} // .StructureValidator
