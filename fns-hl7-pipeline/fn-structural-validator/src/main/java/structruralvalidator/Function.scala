package cdc.ede.hl7.structuralvalidator

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import java.util.Optional;

import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import scala.concurrent._

import cdc.xlr.structurevalidator._


import net.liftweb.json.DefaultFormats
import net.liftweb.json._

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    // Event Hub Trigger 
    @FunctionName("structuralvalidator001")
    def run(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "eventhub002",
            connection = "EventHubConnectionString") 
            message: String,
            context: ExecutionContext): Unit = {

            context.getLogger.info("Event hub triggered a request")
            // context.getLogger.info("message: -> " + message)

            val evHubNameOk = System.getenv("EventHubSendOkName")
            val evHubNameErrs = System.getenv("EventHubSendErrsName")
            val evHubConnStr = System.getenv("EventHubConnectionString")


            implicit val formats = DefaultFormats

            val hl7Messages = parse(message).children

            val validator = StructureValidatorConc()

            for (hl7MessageJ <- hl7Messages) {

                val hl7Message = hl7MessageJ.extract[HL7Message]

                val report = validator.reportMap(hl7Message.content) 

                report match {

                    case Success(report) => {
                        //  context.getLogger.info(s"validation report: --> ${report}")

                        val msgOut = new HL7MessageOut(hl7Message.content, hl7Message.metadata, report)

                        // context.getLogger.info(s"msgOut: --> ${msgOut}")

                        val json = JsonAST.compactRender(Extraction.decompose(msgOut) )

                         EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameOk, message=json)

                    } // .Success

                    case Failure(e) => {
                         context.getLogger.warning(s"validation error: --> ${e.getMessage()}")

                         EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
                    } // .Failure
                        
                } // .match  


            } // .for
          
        
    } // .EventHubTrigger
}
