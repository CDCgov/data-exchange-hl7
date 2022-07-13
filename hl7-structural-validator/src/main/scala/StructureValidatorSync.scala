package cdc.xlr.structurevalidator

import hl7.v2.profile.{Profile, XMLDeserializer}
import hl7.v2.validation.SyncHL7Validator
import hl7.v2.validation.content.{DefaultConformanceContext, ConformanceContext}
import hl7.v2.validation.vs.{ValueSetLibrary, ValueSetLibraryImpl}

//import gov.nist.validation.report.Report
import gov.nist.validation.report.Entry

import scala.collection.JavaConverters._


class StructureValidatorSync(val profile: Profile, val valueSets: ValueSetLibrary, val confContext: ConformanceContext) {


    val validator = new SyncHL7Validator(profile = profile, valueSetLibrary = valueSets, conformanceContext = confContext)

    // returns JSON
    def validate(hl7Message: String): String= {

        val report = validator.check(hl7Message, validator.profile.messages.keySet.head)//.toJson

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

        JsonUtil.toJson(reportMapFiltered)
    } // .validate

} // .StructureValidatorSync

object StructureValidatorSync {
       
  val profileLoader = new ProfileLoaderLocal
 
  val profile = XMLDeserializer.deserialize(profileLoader.profile).get
  val valueSets:ValueSetLibrary = ValueSetLibraryImpl.apply(profileLoader.valueSets).get
  val confContext = DefaultConformanceContext.apply(profileLoader.constraints).get

  def apply() = new StructureValidatorSync(profile = profile, valueSets = valueSets, confContext = confContext)
  
} // .StructureValidatorSync


