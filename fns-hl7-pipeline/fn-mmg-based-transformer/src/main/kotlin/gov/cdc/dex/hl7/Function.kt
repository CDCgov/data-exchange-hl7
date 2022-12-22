package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

// import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonElement
import com.google.gson.JsonArray

import java.util.*

import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.util.DateHelper.toIsoString

import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo

import  gov.cdc.dex.azure.RedisProxy

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

import gov.cdc.dex.metadata.ProcessMetadata

/**
 * Azure function with event hub trigger for the MMG Based Transformer
 * Takes and HL7 message and transforms it to an MMG Based JSON model
 */
class Function {
    
    companion object {
        val PROCESS_STATUS_OK = "PROCESS_MMG_BASED_TRANSFORMER_OK"
        val PROCESS_STATUS_EXCEPTION = "PROCESS_MMG_BASED_TRANSFORMER_EXCEPTION"

        // same in MbtProcessMetadata
        val PROCESS_NAME = "mmgBasedTransformer"
        // val PROCESS_VERSION = "1.0.0"
    } // .companion

    // TODO: Start change back to library once fixed for serialize nulls
    fun Any.toJsonElement():JsonElement {
        val jsonStr = GsonBuilder().serializeNulls().create().toJson(this)
        return JsonParser.parseString(jsonStr)
    }

    fun JsonObject.addArrayElement(arrayName: String, processMD: ProcessMetadata) {
        val currentProcessPayload = this[arrayName]
        if (currentProcessPayload == null) {
            this.add(arrayName,  JsonArray())
        }
        val currentArray = this[arrayName].asJsonArray
        currentArray.add(processMD.toJsonElement())
    }
    // TODO: End. change back to library once fixed for serialize nulls

    @FunctionName("mmgBasedTransformer")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",) 
                message: List<String?>,
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
        message.forEach { singleMessage: String? -> 
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
                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")


                try {
                    // 
                    // Process Message 
                    // ----------------------------------------------

                    // get MMG(s) for the message:
                    val mmgUtil = MmgUtil(redisProxy)
                    val mmgs = mmgUtil.getMMGFromMessage(hl7Content, filePath, messageUUID)
                    mmgs.forEach {
                        context.logger.info("MMG Info for messageUUID: $messageUUID, filePath: $filePath, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}")
                    }

                    val transformer = Transformer(redisProxy)

                    val mmgModelBlocksSingle = transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)

                    val mmgModelBlocksNonSingle = transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)

                    val mmgBasedModel = mmgModelBlocksSingle + mmgModelBlocksNonSingle 
                    // context.logger.info("mmgModel for messageUUID: $messageUUID, filePath: $filePath, mmgModel: --> ${gsonWithNullsOn.toJson(mmgBasedModel)}")
                    context.logger.info("mmgModel for messageUUID: $messageUUID, filePath: $filePath, mmgModel.size: --> ${mmgBasedModel.size}")


                    val processMD = MbtProcessMetadata(status=PROCESS_STATUS_OK, report=mmgBasedModel)
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    metadata.addArrayElement("processes", processMD) 

                    val ehDestination = eventHubSendOkName
                    val outEvent = gsonWithNullsOn.toJson(inputEvent)
                    evHubSender.send(evHubTopicName=ehDestination, message=outEvent)
                    context.logger.info("Processed for MMG Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination")

                } catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    val processMD = MbtProcessMetadata(status=PROCESS_STATUS_EXCEPTION, report=null)
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    metadata.addArrayElement("processes", processMD) 

                    //TODO::  - update retry counts
                    val problem = Problem(PROCESS_NAME, e, false, 0, 0)
                    val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
                    inputEvent.add("summary", summary.toJsonElement())

                    val ehDestination = eventHubSendErrsName

                    evHubSender.send( evHubTopicName=ehDestination, message=gsonWithNullsOn.toJson(inputEvent) )

                    context.logger.info("Processed for MMG Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination")
                    //e.printStackTrace()
                    
                } // .catch

            } catch (e: Exception) {

                context.logger.severe("Exception: Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch
        } // .message.forEach

        redisProxy.getJedisClient().close()
     
    } // .eventHubProcessor

} // .Function

