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


            val hl7Messages = JsonUtil.fromJson[List[HL7Message]](message)
            // context.getLogger.info(s"hl7Messages: --> ${hl7Messages}")


            // for each message received from event hub:
            for (hl7Message <- hl7Messages) {

                val phinProfile = PhinProfileUtil.extract(hl7Message.content)
                context.getLogger.info("Message received PHIN Profile: " + phinProfile.getOrElse("Failure"))

                phinProfile match {

                    case Success(phinProfile) => {

                        val validator = StructureValidatorAsync(ProfileLoaderLocal(phinProfile))
                        val report = validator.report(hl7Message.content) 

                        report match {

                            case Success(report) => {
                                //  context.getLogger.info(s"validation report: --> ${report}")

                                val msgOut = new HL7MessageOut(hl7Message.content, hl7Message.metadata, report)

                                val json = JsonUtil.toJson(msgOut)
                                // context.getLogger.info(s"validation msg json: --> ${json}")

                                EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameOk, message=json)
                            } // .Success

                            case Failure(e) => {

                                context.getLogger.warning(s"validation error: --> ${e.getMessage()}")
                                EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
                            } // .Failure
                                
                        } // .match  
                    } // .Success

                    case Failure(e) => {

                        context.getLogger.warning(s"validation error: --> ${e.getMessage()}")
                        EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
                    } // .Failure

                } // .phinProfile match

            } // .for
        
    } // .EventHubTrigger

} // .Function
