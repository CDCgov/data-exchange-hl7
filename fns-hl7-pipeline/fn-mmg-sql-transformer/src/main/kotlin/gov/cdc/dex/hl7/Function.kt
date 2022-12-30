package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

import com.google.gson.JsonObject
import com.google.gson.JsonParser

import java.util.*

import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.util.DateHelper.toIsoString

import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo

import  gov.cdc.dex.azure.RedisProxy

import gov.cdc.dex.hl7.model.PhinDataType

import com.google.gson.JsonElement
import com.google.gson.JsonArray
import gov.cdc.dex.metadata.ProcessMetadata

/**
 * Azure function with event hub trigger for the MMG SQL Transformer
 * Takes and MMG based model and transforms it to MMG SQL model
 */
class Function {
    
    companion object {
        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        const val TABLES_KEY_NAME = "tables"

        // same in MmgSqlTransProcessMetadata
        const val PROCESS_NAME = "mmgSQLTransformer"
        // const val PROCESS_VERSION = "1.0.0"

        val PROCESS_STATUS_OK = "PROCESS_MMG_SQL_TRANSFORMER_OK"
        val PROCESS_STATUS_EXCEPTION = "PROCESS_MMG_SQL_TRANSFORMER_EXCEPTION"

    } // .companion object

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

    @FunctionName("mmgSQLTransformer")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",) 
                message: List<String?>,
                context: ExecutionContext) {

        // context.logger.info("------ received event: ------> message: --> $message") 

        val startTime =  Date().toIsoString()
        
        // REDIS
        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)

        // GSON
        val gson = Gson()
        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        // Set up the 2 out Event Hubs: OK and Errs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")
        val evHubSender = EventHubSender(evHubConnStr)

        // 
        // Process each Event Hub Message
        // ----------------------------------------------
        message.forEach { singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")

                // Extract from event
                val hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                // Extract the mmg based model from processes
                val processesArr = metadata["processes"].asJsonArray
                val mmgBasedProcessLast = processesArr.filter{ prc ->     
                    val process = prc.asJsonObject 
                    val prcStatus = process["status"].asString
                    prcStatus == "MMG_MODEL_OK"
                }.last() // .mmgBasedProcesses
                val modelJson = mmgBasedProcessLast.asJsonObject["report"].asJsonObject
                // context.logger.info("------ modelJson: ------> $modelJson")
                
                // 
                // Process Message for SQL Model
                // ----------------------------------------------
                try {
                    // get MMGs for the message
                    // ------------------------------------------------------------------------------
                    val mmgUtil = MmgUtil(redisProxy)
                    val mmgsArr = mmgUtil.getMMGFromMessage(hl7Content, filePath, messageUUID)
                    mmgsArr.forEach {
                        context.logger.info("MMG Info for messageUUID: $messageUUID, filePath: $filePath, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}")
                    } // .mmgs

                    // Default Phin Profiles Types
                    // ------------------------------------------------------------------------------
                    val dataTypesFilePath = "/DefaultFieldsProfileX.json"
                    val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
                    val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
                    val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)

                    // Transformer SQL
                    // ------------------------------------------------------------------------------
                    val transformer = TransformerSql()

                    val mmgs = transformer.getMmgsFiltered(mmgsArr)
                    val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks
                    val (mmgBlocksSingle, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
                    val ( mmgElementsSingleNonRepets, mmgElementsSingleRepeats ) = mmgBlocksSingle.flatMap { it.elements }.partition{ !it.isRepeat }

                    // Singles Non Repeats
                    val singlesNonRepeatsModel = transformer.singlesNonRepeatsToSqlModel(mmgElementsSingleNonRepets, profilesMap, modelJson)
                    // context.logger.info("singlesNonRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesNonRepeatsModel)}\n")      

                    // Singles Repeats
                    val singlesRepeatsModel = transformer.singlesRepeatsToSqlModel(mmgElementsSingleRepeats, profilesMap, modelJson)
                    // context.logger.info("singlesRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n") 

                    // Repeated Blocks
                    val repeatedBlocksModel = transformer.repeatedBlocksToSqlModel(mmgBlocksNonSingle, profilesMap, modelJson)
                    // context.logger.info("repeatedBlocksModel: -->\n\n${gsonWithNullsOn.toJson(repeatedBlocksModel)}\n")

                    // Message Profile Identifier
                    val mesageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)
                    // context.logger.info("mesageProfIdModel: -->\n\n${gsonWithNullsOn.toJson(mesageProfIdModel)}\n")
                    // this should not happen
                    if (mesageProfIdModel.size != 3) { throw Exception("Message Profile Identifier does not have 3 parts.")}

                    // Compose the SQL Model from parts
                    val mmgSqlModel = mesageProfIdModel + singlesNonRepeatsModel + mapOf(
                        TABLES_KEY_NAME to singlesRepeatsModel + repeatedBlocksModel,
                    ) // .mmgSqlModel


                    val processMD = MmgSqlTransProcessMetadata(status="MMG_SQL_MODEL_OK", report=mmgSqlModel) 
                    metadata.addArrayElement("processes", processMD)

                    // process time
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    // enable for model
                    val ehDestination = eventHubSendOkName
                    evHubSender.send(evHubTopicName=ehDestination, message=gsonWithNullsOn.toJson(inputEvent))
                    context.logger.info("Processed for MMG Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination")

                } catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    //TODO::  - update retry counts
                    val problem = Problem(PROCESS_NAME, e, false, 0, 0)
                    val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
                    inputEvent.add("summary", summary.toJsonElement())

                    val ehDestination = eventHubSendErrsName

                    evHubSender.send( evHubTopicName=ehDestination, message=gsonWithNullsOn.toJson(inputEvent) )

                    context.logger.info("Processed for MMG SQL Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination")
                } // .catch

            } catch (e: Exception) {

               // message is bad, can't extract fields based on schema expected
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach
        
        redisProxy.getJedisClient().close()
     
    } // .eventHubProcessor

} // .Function

