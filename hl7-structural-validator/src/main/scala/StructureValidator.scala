package cdc.xlr.structurevalidator

import hl7.v2.profile.{Profile, XMLDeserializer}
import hl7.v2.validation.HL7Validator
import hl7.v2.validation.content.{DefaultConformanceContext, ConformanceContext}
import hl7.v2.validation.vs.{ValueSetLibrary, ValueSetLibraryImpl}

//import gov.nist.validation.report.Report
import gov.nist.validation.report.Entry

import scala.collection.JavaConverters._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}


class StructureValidator(val profile: Profile, val valueSets: ValueSetLibrary, val confContext: ConformanceContext) {


    val validator = new HL7Validator(profile = profile, valueSetLibrary = valueSets, conformanceContext = confContext)

    // returns Future[String] the JSON validation report or the error
    def validate(hl7Message: String): Future[String] = {

        validator.validate(hl7Message, validator.profile.messages.keySet.head) transform ({

          case Success( report ) => {

            val reportMap = report.getEntries().asScala.mapValues(_.asScala.toList)

            val errClsf = "Error"
            val warnClsf = "Warning"

            val reportMapFiltered = Map(
              // structure
              "structureErrorsCount" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(errClsf)).size,
              "structureWarningsCount" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(warnClsf)).size,

              // value set
              "valueSetErrorsCount" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(errClsf)).size,
              "valueSetWarningsCount" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(warnClsf)).size,

              // content
              "contentErrorsCount" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(errClsf)).size,
              "contentWarningsCount" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(warnClsf)).size,

              // details 
              "structureErrors" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(errClsf))/*.map(_.toText)*/,
              "structureWarnings" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(warnClsf))/*.map(_.toText)*/,

              "valueSetErrors" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(errClsf))/*.map(_.toText)*/,
              "valueSetWarnings" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(warnClsf))/*.map(_.toText)*/,

              "contentErrors" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(errClsf))/*.map(_.toText)*/,
              "contentWarnings" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(warnClsf))/*.map(_.toText)*/,
            ) // .Map

           Success( JsonUtil.toJson(reportMapFiltered) )
          } // .Success 

          case Failure( err ) => Failure( err ) // err.getMessage
            
        }) // .transform

    } // .validate

} // .StructureValidator

object StructureValidator {
       
  val profileLoader = new ProfileLoaderLocal
 
  val profile = XMLDeserializer.deserialize(profileLoader.profile).get
  val valueSets:ValueSetLibrary = ValueSetLibraryImpl.apply(profileLoader.valueSets).get
  val confContext = DefaultConformanceContext.apply(profileLoader.constraints).get

  def apply() = new StructureValidator(profile = profile, valueSets = valueSets, confContext = confContext)
  
} // .StructureValidator


