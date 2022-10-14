//package gov.cdc.dex.hl7
//
//package cdc.ede.hl7.structuralvalidator
//
//import cdc.xlr.structurevalidator._
//import com.microsoft.azure.functions.ExecutionContext
//import com.microsoft.azure.functions.annotation.{EventHubTrigger, FunctionName}
//import net.liftweb.json._
//
//import scala.concurrent._
//import scala.concurrent.duration._
//import scala.util.{Failure, Success}
//
///**
// * Azure Functions with Event Hub Trigger.
// */
//class Function {
//    // Event Hub Trigger
//    @FunctionName("structuralvalidator001")
//    def run(
//    @EventHubTrigger(
//        name = "msg",
//        eventHubName = "hl7-recdeb-ok",
//        connection = "EventHubConnectionString")
//    message: String,
//    context: ExecutionContext): Unit = {
//
//        context.getLogger.info("Event hub triggered a request")
//        // context.getLogger.info("message: -> " + message)
//
//        val evHubNameOk = System.getenv("EventHubSendOkName")
//        val evHubNameErrs = System.getenv("EventHubSendErrsName")
//        val evHubConnStr = System.getenv("EventHubConnectionString")
//
//        implicit val formats = DefaultFormats
//
//        val hl7Messages = parse(message).children
//
//        // for each message received from event hub:
//        for (hl7MessageJ <- hl7Messages) {hl7Messages
//
//            val hl7Message = hl7MessageJ.extract[HL7Message]
//
//            val phinProfile = PhinProfileUtil.extract(hl7Message.content)
//            context.getLogger.info("Message received PHIN Profile: " + phinProfile.getOrElse("Failure"))
//
//            phinProfile match {
//
//                case Success(phinProfile) => {
//
//                val validator = StructureValidatorAsync(ProfileLoaderLocal(phinProfile))
//                val report = validator.reportMap(hl7Message.content)
//
//                report match {
//
//                    case Success(report) => {
//                    //  context.getLogger.info(s"validation report: --> ${report}")
//
//                    val msgOut = new HL7MessageOut(hl7Message.content, hl7Message.metadata, report)
//                    // context.getLogger.info(s"msgOut: --> ${msgOut}")
//
//                    val json = JsonAST.compactRender(Extraction.decompose(msgOut) )
//
//                    EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameOk, message=json)
//                } // .Success
//
//                    case Failure(e) => {
//
//                    context.getLogger.warning(s"validation error: --> ${e.getMessage()}")
//                    EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
//                } // .Failure
//
//                } // .match
//            } // .Success
//
//                case Failure(e) => {
//
//                context.getLogger.warning(s"validation error: --> ${e.getMessage()}")
//                EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameErrs, message=e.getMessage())
//            } // .Failure
//
//            } // .phinProfile match
//
//        } // .for
//
//    } // .EventHubTrigger
//
//} // .Function
