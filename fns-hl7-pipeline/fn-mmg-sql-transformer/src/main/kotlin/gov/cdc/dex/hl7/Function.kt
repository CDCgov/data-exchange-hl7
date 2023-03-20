package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.HL7StaticParser
import java.util.*

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

        const val PROCESS_STATUS_OK = "PROCESS_MMG_SQL_TRANSFORMER_OK"
        const val PROCESS_STATUS_EXCEPTION = "PROCESS_MMG_SQL_TRANSFORMER_EXCEPTION"
        const val PREVIOUS_PROCESS_NAME = "mmgBasedTransformer"

    } // .companion object


    @FunctionName("mmgSQLTransformer")
    fun eventHubProcessor(
        @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",) 
                message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
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
        message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
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
                val fileName = filePath.split("/").last()
                val messageUUID = inputEvent["message_uuid"].asString
                val messageInfo = inputEvent["message_info"].asJsonObject

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")
                context.logger.info("message info: ${inputEvent["message_info"]}")
                // Extract the mmg based model from processes
                val processesArr = metadata["processes"].asJsonArray
                val mmgBasedProcessLast = processesArr.last { prc ->
                    val process = prc.asJsonObject
                    val prcName = process["process_name"].asString
                    prcName == PREVIOUS_PROCESS_NAME
                } // .mmgBasedProcesses
                val modelJson = mmgBasedProcessLast.asJsonObject["report"].asJsonObject
             //   context.logger.info("------ modelJson: ------> $modelJson")
                
                // 
                // Process Message for SQL Model
                // ----------------------------------------------
                try {
                    // get MMGs for the message
                    // ------------------------------------------------------------------------------
                    val mmgUtil = MmgUtil(redisProxy)
                    val eventCode = messageInfo["event_code"].asString
                    val jurisdictionCode = messageInfo["reporting_jurisdiction"].asString
                    if (eventCode.isEmpty() || jurisdictionCode.isEmpty()) {
                        throw Exception("Unable to process message due to missing required information: Event Code is '$eventCode', Jurisdiction Code is '$jurisdictionCode'")
                    }

                    val mmgKeyNames :Array<String> = JsonHelper.getStringArrayFromJsonArray(messageInfo["mmgs"].asJsonArray)
                    context.logger.info("MMG List from MessageInfo: ${mmgKeyNames.contentToString()}")
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
                    mmgs.forEach {
                        context.logger.info("MMG Info for fileName: $fileName, messageUUID: $messageUUID, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}")
                    } // .mmgs

                    // Default Phin Profiles Types
                    // ------------------------------------------------------------------------------
                    val dataTypesFilePath = "/DefaultFieldsProfileX.json"
                    val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath)?.readText()
                    val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
                    val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)

                    // Transformer SQL
                    // ------------------------------------------------------------------------------
                    context.logger.info("Initializing transformer")
                    val transformer = TransformerSql()
                    context.logger.info("Filtering MMGs")
                    val mmgsFiltered = transformer.getMmgsFiltered(mmgs)
                    context.logger.info("Getting MMG Blocks")
                    val mmgBlocks = mmgsFiltered.flatMap { it.blocks } // .mmgBlocks
                    context.logger.info("Partitioning blocks")
                    val (mmgBlocksSingle, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
                    context.logger.info("Mapping elements")
                    val ( mmgElementsSingleNonRepeats, mmgElementsSingleRepeats ) = mmgBlocksSingle.flatMap { it.elements }.partition{ !it.isRepeat }

                    // Singles Non Repeats
                    val singlesNonRepeatsModel = transformer.singlesNonRepeatsToSqlModel(mmgElementsSingleNonRepeats, profilesMap, modelJson)
              //       context.logger.info("singlesNonRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesNonRepeatsModel)}\n")

                    // Singles Repeats
                    val singlesRepeatsModel = transformer.singlesRepeatsToSqlModel(mmgElementsSingleRepeats, profilesMap, modelJson)
              //       context.logger.info("singlesRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n")

                    // Repeated Blocks
                    val repeatedBlocksModel = transformer.repeatedBlocksToSqlModel(mmgBlocksNonSingle, profilesMap, modelJson)
               //      context.logger.info("repeatedBlocksModel: -->\n\n${gsonWithNullsOn.toJson(repeatedBlocksModel)}\n")

                    // Message Profile Identifier
                    val messageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)
               //      context.logger.info("messageProfIdModel: -->\n\n${gsonWithNullsOn.toJson(messageProfIdModel)}\n")

                    // Compose the SQL Model from parts
                    val mmgSqlModel = messageProfIdModel + singlesNonRepeatsModel + mapOf(
                        TABLES_KEY_NAME to singlesRepeatsModel + repeatedBlocksModel,
                    ) // .mmgSqlModel

                    updateMetadataAndDeliver(startTime, metadata, PROCESS_STATUS_OK, mmgSqlModel, eventHubMD[messageIndex],
                        evHubSender, eventHubSendOkName, gsonWithNullsOn, inputEvent,   null, mmgKeyNames.asList())
                    context.logger.info("Processed OK for MMG-SQL fileName: $fileName, messageUUID: $messageUUID, ehDestination: $eventHubSendOkName")

                } catch (e: Exception) {
                    context.logger.severe("Exception processing transformation: Unable to process Message fileName: $fileName, messageUUID: $messageUUID, due to exception: ${e.message}")
                    updateMetadataAndDeliver(startTime, metadata, PROCESS_STATUS_EXCEPTION, null, eventHubMD[messageIndex],
                        evHubSender, eventHubSendErrsName, gsonWithNullsOn, inputEvent, e, listOf()
                    )
                    context.logger.info("Processed ERROR for MMG-SQL fileName: $fileName, messageUUID: $messageUUID, ehDestination: $eventHubSendErrsName")
                } // .catch

            } catch (e: Exception) {

               // message is bad, can't extract fields based on schema expected
                context.logger.severe("Exception processing event hub message: Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach

    } // .eventHubProcessor
    private fun extractValue(msg: String, path: String): String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get()
        else ""
    } // .extractValue

    private fun updateMetadataAndDeliver(startTime: String, metadata: JsonObject, status: String, report: Map<String, Any?>?, eventHubMD: EventHubMetadata,
        evHubSender: EventHubSender, evTopicName: String, gsonWithNullsOn: Gson, inputEvent: JsonObject, exception: Exception?, config: List<String>) {

        val processMD = MmgSqlTransProcessMetadata(status=status, report=report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        metadata.addArrayElement("processes", processMD)

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(PROCESS_NAME, exception, false, 0, 0)
            val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
            inputEvent.add("summary", summary.toJsonElement())
        } else {
            inputEvent.add("summary", (SummaryInfo(status, null).toJsonElement()))
        }
        // enable for model
        val inputEventOut = gsonWithNullsOn.toJson(inputEvent)
        evHubSender.send(
            evHubTopicName = evTopicName,
            message = inputEventOut
        )

    }
} // .Function

