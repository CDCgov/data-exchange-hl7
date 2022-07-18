package gov.cdc.dataexchange.entModel

import cdc.xlr.structurevalidator._
import gov.nist.validation.report.{ Report, Entry }
import scala.collection.JavaConverters._

import scala.util.{Try, Failure, Success}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit


case class MessageHL7 (
  /*override*/ val content: String,
  val structureValidationReport: Option[Map[String, List[Entry]]] = None,
  val contentValidationReport: Option[String] = None,

  // Silver
  // TODO: remove and change to Lake Of Segments
  val obxsEpi: Option[Seq[(String, String, String, String)]] = None,
  val obxsNonEpi:  Option[Seq[(String, String, (String, Int), (String, Int))]] = None,
  val otherSegments:   Option[Seq[(String, String, (String, Int))]] = None,

  // TODO: only keep segments and no loger above 
  val segments:  Option[Seq[Tuple3[String, Seq[String], Seq[String]]]] = None,

  // Gold
  val entModel: Option[Map[String, String]] = None,

  // Config
  val mmgSeq:  Option[Seq[Seq[String]]] = None,
  val vocabularyEntries: Option[Seq[String]]  = None,
  val vocabulary: Option[Map[String, Seq[(String, String, String)]]]  = None

) /*extends Message*/ {

  def validateStructure(message: MessageHL7): MessageHL7 = {
    
    val validator = StructureValidator()

    Try( Await.result(validator.validateToReport(message.content), Duration(2, TimeUnit.SECONDS)) ) match {

      case Success( report ) => {
        
        val reportMap = report.getEntries().asScala.mapValues(_.asScala.toList)

        
            val errClsf = "Error"
            val warnClsf = "Warning"

            val reportMapFiltered = Map(
              // details 
              "structureErrors" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(errClsf))/*.map(_.toText)*/,
              "structureWarnings" -> reportMap("structure").filter(_.getClassification.equalsIgnoreCase(warnClsf))/*.map(_.toText)*/,

              "valueSetErrors" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(errClsf))/*.map(_.toText)*/,
              "valueSetWarnings" -> reportMap("value-set").filter(_.getClassification.equalsIgnoreCase(warnClsf))/*.map(_.toText)*/,

              "contentErrors" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(errClsf))/*.map(_.toText)*/,
              "contentWarnings" -> reportMap("content").filter(_.getClassification.equalsIgnoreCase(warnClsf))/*.map(_.toText)*/,
            ) // .Map 

         new MessageHL7(message.content, Option(reportMapFiltered)) // message with report
      } // .Success

      case Failure( err ) => {
        println("Error structure validation: ", err.getMessage )
        message // return message as is
      } // .Failure

    } // .Try   

  } // .validateStructure 

  def isValidStructure(message: Message): Option[Message] = Option(message) 

  def validateContent(message: Message): Message = ???

  def isValidContent(message: Message): Option[Message] = Option(message) 

  def transformToSegmLake(message: Message): Message = ???

  // TODO: remove once MessageHL7 changes
  // def transformToObxLake(message: Message): Message = ???

  def transformToEntModel(message: Message): Message = ???

} // .MessageHL7


// import spray.json._
// import DefaultJsonProtocol._ 


// trait MessageHL7JsonProtocol extends DefaultJsonProtocol {

//   implicit val messageHL7Format: JsonFormat[MessageHL7] = jsonFormat11(MessageHL7)
//   implicit val entryFormat: JsonFormat[Entry] = jsonFormat1(Entry)

// } // .MessageHL7JsonProtocol