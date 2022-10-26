package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.hl7.model.HL7Message
import com.google.gson.Gson 

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    @FunctionName("mmgvalidator001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",) 
                message: String?,
                context: ExecutionContext) {
        
        // context.logger.info("received event: --> $message") 

        // set up the 2 out event hubs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendOkName")

        val eventHubSenderOk = EvHubSender(evHubName=eventHubSendOkName, evHubConnStr=evHubConnStr)
        val eventHubSenderErrs = EvHubSender(evHubName=eventHubSendErrsName, evHubConnStr=evHubConnStr)

        // when testing local message for dev
        // val hl7TestMessage = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()

        //
        // Event Hub -> receive events
        // -------------------------------------
        val eventArr = Gson().fromJson(message, Array<HL7Message>::class.java)

        //
        // For each Event received
        // -------------------------------------
        for (hl7Message in eventArr) {

            val messageUUID = hl7Message.metadata.messageUUID
            val fileName = hl7Message.metadata.fileName

            context.logger.info("Received and Processing Message: messageUUID $messageUUID, fileName: $fileName")


            try {
                // get MMG(s) for the message:
                val mmgs = MmgUtil.getMMGFromMessage(hl7Message.content)
                mmgs.forEach {
                    context.logger.info("${it.name} BLOCKS: --> ${it.blocks.size}")
                }

                val mmgValidator = MmgValidator( hl7Message.content, mmgs )
                val validationReport = mmgValidator.validate()
                context.logger.info("validationReport: --> " + validationReport.size)

                val otherSegmentsValidator = MmgValidatorOtherSegments( hl7Message.content, mmgs )
                val validationReportOtherSegments = otherSegmentsValidator.validate()
                context.logger.info("validationReportOtherSegments: --> " + validationReportOtherSegments.size)

                val validationReportFull = validationReport + validationReportOtherSegments
                context.logger.info("validationReportFull: --> " + validationReportFull.size)

                // adding the content validation report to received message 
                // and sending to next event hub
                hl7Message.contentValidationReport = validationReportFull 
                val json = Gson().toJson(hl7Message)

                eventHubSenderOk.send(message=json)
                // TODO: are MMG validation errors ok and message still goes to 
                context.logger.info("Processed for MMG validated messageUUID: $messageUUID, fileName: $fileName, ehDestination: $eventHubSendOkName")

               // println("ValidationReport:\t  $validationReportFull")
//             } catch (e: MessageNotRecognizableException) {
//                 //Handle error - send Message to Dead Letter.
//                 eventHubSenderErrs.send( message=e.msg )
//             } catch (e: InvalidMessageException) {
//                 eventHubSenderErrs.send( message=e.msg )
            } catch (e: Exception) {
                //TODO:: Define Error Information to be pushed to error queues.
                e.message ?.let{
                    eventHubSenderErrs.send( message=e.message!!)
                    context.logger.info("Processed for MMG validated with Exception, messageUUID: $messageUUID, fileName: $fileName, ehDestination: $eventHubSendErrsName")
                }
               
            } // .catch

        } // .for

    } // .eventHubProcessor

} // .Function

