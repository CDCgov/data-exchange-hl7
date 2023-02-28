package gov.cdc.dex.hl7

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.HL7StaticParser
import java.util.*


/**
 * Azure function with event hub trigger for the MMG Based Transformer
 * Takes and HL7 message and transforms it to an MMG Based JSON model
 */
class Function {
    
    companion object {
        const val PROCESS_STATUS_OK = "PROCESS_MMG_BASED_TRANSFORMER_OK"
        const val PROCESS_STATUS_EXCEPTION = "PROCESS_MMG_BASED_TRANSFORMER_EXCEPTION"

        // same in MbtProcessMetadata
        const val PROCESS_NAME = "mmgBasedTransformer"
        // val PROCESS_VERSION = "1.0.0"
    } // .companion

    private fun extractValue(msg: String, path: String):String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get() //throw InvalidMessageException("Error extracting $path from HL7 message")
        else ""
    }
    @FunctionName("mmgBasedTransformer")
    fun eventHubProcessor(
        @EventHubTrigger(
                name = "msg",
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",
            )
                message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {


        val startTime =  Date().toIsoString()

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        
        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)

        // context.logger.info("received event: --> $message") 
        val gsonWithNullsOn = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        // set up the 2 out event hubs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")

        val evHubSender = EventHubSender(evHubConnStr)

        // 
        // Process each Event Hub Message
        // ----------------------------------------------
        message.forEachIndexed{
            messageIndex : Int, singleMessage: String? ->
            try {
                // 
                // Extract from Event Hub Message 
                // ----------------------------------------------

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("singleMessage: --> $singleMessage")
                val hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString
                val messageInfo = inputEvent["message_info"].asJsonObject

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")
                val mmgUtil = MmgUtil(redisProxy)

                try {
                    // 
                    // Process Message 
                    // ----------------------------------------------

                    // get MMG(s) for the message:
                    val eventCode = messageInfo["event_code"].asString
                    val jurisdictionCode = messageInfo["reporting_jurisdiction"].asString
                    if (eventCode.isEmpty() || jurisdictionCode.isEmpty()) {
                        throw Exception("Unable to process message due to missing required information: Event Code is '$eventCode', Jurisdiction Code is '$jurisdictionCode'")
                    }

                    val mmgKeyNames :Array<String> = JsonHelper.getStringArrayFromJsonArray(messageInfo["mmgs"].asJsonArray)
                 //   context.logger.info("MMG List from MessageInfo: ${mmgKeyNames.contentToString()}")
                    val mmgs = try {
                        if (mmgKeyNames.isNotEmpty()) {
                            mmgUtil.getMMGs(mmgKeyNames)
                        } else {
                            val mshProfile = extractValue(hl7Content, "MSH-21[2].1")
                            val mshCondition = extractValue(hl7Content, "MSH-21[3].1")
                            mmgUtil.getMMGs(mshProfile, mshCondition, eventCode, jurisdictionCode)
                        }
                    } catch (e: NullPointerException) {
                        arrayOf()
                    }
                    if (mmgs.isEmpty()) {
                        throw Exception ("Unable to find MMGs for message.")
                    }
                    mmgs.forEach { context.logger.info("MMG Info for messageUUID: $messageUUID, " +
                            "filePath: $filePath, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}") }
                    val transformer = Transformer(redisProxy)
                    val mmgModelBlocksSingle = transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)
                    val mmgModelBlocksNonSingle = transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)
                    val mmgBasedModel = mmgModelBlocksSingle + mmgModelBlocksNonSingle
                   // context.logger.info("mmgModel for messageUUID: $messageUUID, filePath: $filePath, mmgModel: --> ${gsonWithNullsOn.toJson(mmgBasedModel)}")
                    context.logger.info("mmgModel for messageUUID: $messageUUID, filePath: $filePath, mmgModel.size: --> ${mmgBasedModel.size}")

                    val processMD = MbtProcessMetadata(status = PROCESS_STATUS_OK,
                        report = mmgBasedModel,
                        eventHubMD = eventHubMD[messageIndex])
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    metadata.addArrayElement("processes", processMD)

                    val outEvent = gsonWithNullsOn.toJson(inputEvent)
                    evHubSender.send(evHubTopicName = eventHubSendOkName, message = outEvent)
                    context.logger.info("Processed for MMG Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendOkName")

                } catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    val processMD = MbtProcessMetadata(status=PROCESS_STATUS_EXCEPTION,
                        report=null,
                        eventHubMD = eventHubMD[messageIndex])
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    metadata.addArrayElement("processes", processMD) 

                    //TODO::  - update retry counts
                    val problem = Problem(PROCESS_NAME, e, false, 0, 0)
                    val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
                    inputEvent.add("summary", summary.toJsonElement())

                    evHubSender.send(
                        evHubTopicName = eventHubSendErrsName,
                        message = gsonWithNullsOn.toJson(inputEvent)
                    )

                    context.logger.info("Processed for MMG Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendErrsName")
                    //e.printStackTrace()
                    
                } // .catch

            } catch (e: Exception) {

                context.logger.severe("Exception: Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch
        } // .message.forEach

       // redisProxy.getJedisClient().close()
     
    } // .eventHubProcessor

} // .Function

