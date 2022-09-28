package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.hl7.exception.MessageNotRecognizableException


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    @FunctionName("mmgvalidator001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                // TODO:
                eventHubName = "eventhub001",
                connection = "EventHubConnectionString") 
                message: String?,
            context: ExecutionContext) {

        // set up the 2 out event hubs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val evHubNameOk = System.getenv("EventHubSendOkName")
        val evHubNameErrs = System.getenv("EventHubSendErrsName")
        

        // context.logger.info("message: --> " + message)
        // TODO: change to read message from Even Hub, validate hl7Message.content
        val hl7TestMessage = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()

        try {
            // get MMG(s) for the message:
            val mmgs = MmgUtil.getMMGFromMessage(hl7TestMessage)
            mmgs.forEach {
                context.logger.info("${it.name} BLOCKS: --> ${it.blocks.size}")
            }

            val mmgValidator = MmgValidator(context, hl7TestMessage, mmgs)
            val validationReport = mmgValidator.validate()
            // EvHubUtil.evHubSend(evHubConnStr = evHubConnStr, evHubName = evHubNameOk, message=json)

            context.logger.info("validationReport: --> " + validationReport.size)

            // TODO:...
            // //
            // // Event Hub -> receive events
            // // -------------------------------------
            // val eventArr = Gson().fromJson(message, Array<HL7Message>::class.java)

            // //
            // // For each Event received
            // // -------------------------------------
            // for (event in eventArr) {

            //     context.logger.info("event received: --> " + event)

            // } // .for
        } catch (e: MessageNotRecognizableException) {
            //Handle error - send Message to Dead Letter.
        } catch (e: InvalidMessageException) {

        }

    } // .eventHubProcessor

} // .Function

