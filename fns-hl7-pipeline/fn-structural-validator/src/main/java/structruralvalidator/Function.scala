package cdc.ede.hl7.structuralvalidator

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import scala.util.{Failure, Success, Try}
import cdc.xlr.structurevalidator._
import com.google.gson.{Gson, GsonBuilder, JsonElement, JsonParser}

import java.util
import scala.collection.BitSet.empty.to

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
  // Event Hub Trigger
  @FunctionName("structuralvalidator001")
  def run(
           @EventHubTrigger(
             name = "msg",
             eventHubName = "%EventHubReceiveName%",
             connection = "EventHubConnectionString")
           message: String,
           context: ExecutionContext): Unit = {

    context.getLogger.info("Event hub triggered a request")
    // context.getLogger.info("message: -> " + message)

    val evHubNameOk = System.getenv("EventHubSendOkName")
    val evHubNameErrs = System.getenv("EventHubSendErrsName")
    val evHubConnStr = System.getenv("EventHubConnectionString")


    val elem = JsonParser.parseString(message.toString())
    // context.getLogger.info("Json Array size:" + elem.getAsJsonArray)
    val elemArray = elem.getAsJsonArray()
    // var msgObj = elem.getAsJsonArray.get(0).getAsJsonObject.get("content").getAsString
    // var metadataObj = elem.getAsJsonArray.get(0).getAsJsonObject.get("metadata").toString
    // context.getLogger.info(s"metadataObj : ${metadataObj}")

    // for each message received from event hub:
    var itor: util.Iterator[JsonElement] = elemArray.iterator()
    while (itor.hasNext) {
      var msgElem = itor.next()
      var msgObj = msgElem.getAsJsonObject.get("content").getAsString
      var metadataObj = msgElem.getAsJsonObject.get("metadata").toString

      val phinProfile = PhinProfileUtil.extract(msgObj)
      context.getLogger.info("Message received PHIN Profile: " + phinProfile.getOrElse("Failure"))

      phinProfile match {

        case Success(phinProfile) => {

          val validator = StructureValidatorAsync(ProfileLoaderLocal(phinProfile))
          val report = validator.report(msgObj)

          report match {

            case Success(report) => {
              //context.getLogger.info(s"validation report: --> ${report}")
              val msgOut = new HL7MessageOut(msgObj, metadataObj, report)

              var json = new Gson().toJson(msgOut)
              //context.getLogger.info(s"validation msg json: --> ${json}")

              EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameOk, message = json)
            } // .Success

            case Failure(e) => {

              context.getLogger.warning(s"validation error1: --> ${e.getMessage()}")
              EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message = e.getMessage())
            } // .Failure

          } // .match
        } // .Success

        case Failure(e) => {

          context.getLogger.warning(s"validation error2: --> ${e.getMessage()}")
          EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message = e.getMessage())
        } // .Failure

      } // .phinProfile match

    } // while
  } // .EventHubTrigger

} // .Function
