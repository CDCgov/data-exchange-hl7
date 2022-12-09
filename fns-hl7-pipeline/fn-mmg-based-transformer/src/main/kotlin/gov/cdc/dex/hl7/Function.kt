package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.google.gson.JsonObject
import com.google.gson.JsonParser

import java.util.*

import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.util.DateHelper.toIsoString

import gov.cdc.dex.metadata.Problem
import  gov.cdc.dex.azure.RedisProxy

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    
    companion object {
    } // .companion

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
        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        // set up the 2 out event hubs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        // val eventHubSendErrsName = System.getenv("EventHubSendErrsName")

        val evHubSender = EventHubSender(evHubConnStr)

        // 
        // Process each Event Hub Message
        // ----------------------------------------------
        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")

            try {
                val hl7ContentBase64 = inputEvent["content"].asString

                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)

                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject

                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString
                
                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")
                
                // 
                // Process Message 
                // ----------------------------------------------
                try {
                    // get MMG(s) for the message:
                    val mmgUtil = MmgUtil(redisProxy)
                    val mmgs = mmgUtil.getMMGFromMessage(hl7Content, filePath, messageUUID)
                    mmgs.forEach {
                        context.logger.info("MMG Info for messageUUID: $messageUUID, filePath: $filePath, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}")
                    }

                    val transformer = Transformer(redisProxy)

                    val mmgModelBlocksSingle = transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)

                    val mmgModelBlocksNonSingle = transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)

                    val mmgModel = mmgModelBlocksSingle + mmgModelBlocksNonSingle 
                    context.logger.info("mmgModel for messageUUID: $messageUUID, filePath: $filePath, mmgModel: --> ${gsonWithNullsOn.toJson(mmgModel)}")

                    val processMD = MbtProcessMetadata(status="MMG_MODEL_OK", report=mmgModel) // TODO: MMG_MODEL_OK
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)
                    
                    val ehDestination = eventHubSendOkName
                    evHubSender.send(evHubTopicName=ehDestination, message=gsonWithNullsOn.toJson(inputEvent))
                    context.logger.info("Processed for MMG Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination")

                   

                } catch (e: Exception) {
                    
                    
                    context.logger.severe("Try 1: Unable to process Message due to exception: ${e.message}")

                    // val problem = Problem(MMG_VALIDATOR, e, false, 0, 0)
                    // val summary = SummaryInfo(STATUS_ERROR, problem)
                    // inputEvent.add("summary", summary.toJsonElement())
                    // evHubSender.send( evHubTopicName=eventHubSendErrsName, message=Gson().toJson(inputEvent) )
                    // throw  Exception("Unable to process Message messageUUID: $messageUUID, filePath: $filePath due to exception: ${e.message}")

                    e.printStackTrace()
                } 
    


            } catch (e: Exception) {
                //TODO::  - update retry counts
                context.logger.severe("Try 2: Unable to process Message due to exception: ${e.message}")

                // val problem = Problem(MMG_VALIDATOR, e, false, 0, 0)
                // val summary = SummaryInfo(STATUS_ERROR, problem)
                // inputEvent.add("summary", summary.toJsonElement())

                // evHubSender.send( evHubTopicName=eventHubSendErrsName, message=Gson().toJson(inputEvent) )
                e.printStackTrace()
            } finally {
                redisProxy.getJedisClient().close()
            }
        } // .message.forEach

     
    } // .eventHubProcessor

} // .Function

