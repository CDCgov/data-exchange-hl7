package cdc.ede.hl7.structuralvalidator

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import java.util.Optional
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.concurrent._
import cdc.xlr.structurevalidator._

import net.liftweb.json.DefaultFormats
import net.liftweb.json._

import scala.collection.mutable.ListBuffer

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    // Event Hub Trigger 
    @FunctionName("structuralvalidator001")
    def run(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "hl7-recdeb-ok",
            connection = "EventHubConnectionString") 
            message: String,
            context: ExecutionContext): Unit = {

            context.getLogger.info("Event hub triggered a request")
           //  context.getLogger.info("message: -> " + message)

            val evHubNameOk = System.getenv("EventHubSendOkName")
            val evHubNameErrs = System.getenv("EventHubSendErrsName")
            val evHubConnStr = System.getenv("EventHubConnectionString")

            implicit val formats = DefaultFormats

            val hl7Messages = parse(message).children

            // for each message received from event hub:
            for (hl7MessageJ <- hl7Messages) {

                val hl7Message = hl7MessageJ.extract[HL7Message]

                val phinProfile = PhinProfileUtil.extract(hl7Message.content)
                context.getLogger.info("Message received PHIN Profile: " + phinProfile.getOrElse("Failure"))

                phinProfile match {

                    case Success(phinProfile) =>

                      val validator = StructureValidatorAsync(ProfileLoaderLocal(phinProfile))
                      val report = validator.reportMap(hl7Message.content)

                      report match {

                          case Success(report) =>
                           // context.getLogger.info(s"validation report: --> ${report}")
                            var jReport = JsonConverter.toJson(report)

                            val msgOut = new HL7MessageOut(hl7Message.content, hl7Message.metadata, jReport)
                            //context.getLogger.info(s"msgOut: --> ${msgOut}")

                            val json = JsonAST.compactRender(Extraction.decompose(msgOut) )
                            context.getLogger.info(s"JSON MSG: --> ${json}")

                            EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameOk, message=json) // .Success
                            context.getLogger.info("Message added into hl7-structure-ok eventhub")

                          case Failure(e) => {

                              context.getLogger.warning(s"validation1 error: --> ${e.getMessage()}")
                              EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
                          } // .Failure

                      } // .match // .Success

                    case Failure(e) => {

                        context.getLogger.warning(s"validation2 error: --> ${e.getMessage()}")
                        EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
                    } // .Failure

                } // .phinProfile match

            } // .for
        
    } // .EventHubTrigger

  object JsonConverter {
    def toJson(o: Any): String = {
       var json = new ListBuffer[String]()
        o match {
        case m: Map[_, _] => {
          for ((k, v) <- m) {
            var key = escape(k.asInstanceOf[String])
           // println("V....... "+ key  +"--- " +  v)
            v match {
              case a: Map[_, _] => json += "\"" + key + "\":" + toJson(a)
              case a: List[_] => json += "\"" + key + "\":" + toJson(a)
              case a: Int => json += "\"" + key + "\":" + a
              case a: Boolean => json += "\"" + key + "\":" + a
              case a: String => json += "\"" + key + "\":\"" + escape(a) + "\""
              case _ => ;
            }
          }
        }
        case m: List[_] => {
          var list = new ListBuffer[String]()
          for (el <- m) {
           // println("List el..."+ el)
            el match {
              case a: Map[_, _] => list += toJson(a)
              case a: List[_] => list += toJson(a)
              case a: Int => list += a.toString()
              case a: Boolean => list += a.toString()
              case a: String => list += "\"" + escape(a) + "\""
              case _ =>  ;
            }
            list += el.toString()
          }
          return "[" + list.mkString(",") + "]"
        }
        case _ => ;
      }
      return "{" + json.mkString(",") + "}"
    }

    private def escape(s: String): String = {
      return s.replaceAll("\"", "\\\\\"");
    }
  }


} // .Function
