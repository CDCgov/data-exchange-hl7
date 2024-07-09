package gov.cdc.dataexchange

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gov.cdc.dex.reports.*

class ReportTransformer {
    fun mapDataToBaseReport(record: String) : PSBaseReport {
        val inputEvent = JsonParser.parseString(record).asJsonObject
        val messageMetadata = inputEvent["message_metadata"]?.asJsonObject
        val routingMetadata = inputEvent["routing_metadata"].asJsonObject
        val stageMetadata = inputEvent["stage"].asJsonObject
        val summary = inputEvent["summary"].asJsonObject

        val psMessageMetadata = mapMessageMetadata(messageMetadata)
        val psStageInfo = mapStageInfo(stageMetadata, summary)
        val psStageContent = mapStageContent(stageMetadata, routingMetadata)
        return PSBaseReport(
            uploadId = routingMetadata["upload_id"].asString,
            userId = routingMetadata["user_id"]?.asString,
            dataStreamId = routingMetadata["data_stream_id"].asString,
            dataStreamRoute = routingMetadata["data_stream_route"].asString,
            jurisdiction = routingMetadata["jurisdiction"]?.asString,
            senderId = routingMetadata["sender_id"].asString,
            dataProducerId = routingMetadata["data_producer_id"].asString,
            dexIngestTimestamp = routingMetadata["dex_ingest_datetime"].asString,
            messageMetadata = psMessageMetadata,
            stageInfo = psStageInfo,
            content = psStageContent

        )
    }

    fun mapMessageMetadata(messageMetadata: JsonObject?) : MessageMetadata? {
        if (messageMetadata == null) return null
        return MessageMetadata(
            messageUuid = messageMetadata["message_uuid"].asString,
            messageHash = messageMetadata["message_hash"].asString,
            aggregation = if (messageMetadata["single_or_batch"].asString == "BATCH") AggregationType.BATCH else AggregationType.SINGLE,
            messageIndex = messageMetadata["message_index"].asBigInteger.toInt()

        )
    }

    fun mapStageInfo(stageMetadata: JsonObject, summaryInfo: JsonObject) : StageInfo {
        val problem = summaryInfo["problem"].asJsonObject
        var status = stageMetadata["status"]?.asString
        if (status == null) status = "SUCCESS"

        return StageInfo(
            stage = stageMetadata["stage_name"].asString,
            version = stageMetadata["stage_version"].asString,
            status = if (status == "SUCCESS") StageStatus.SUCCESS else StageStatus.FAILURE,
            issues = if (problem.isJsonNull) null else listOf(
                Issue(
                level = IssueLevel.ERROR,
                message = problem["error_message"].asString
            )
            ),
            startProcessingTime = stageMetadata["start_processing_time"].asString,
            endProcessingTime = stageMetadata["end_processing_time"].asString
        )
    }

    fun mapStageContent(stageMetadata: JsonObject, routingMetadata: JsonObject) : StageContent? {
        val stage = stageMetadata["stage_name"].asString
        when (stage) {
            "RECEIVER" -> { return mapReceiverReport(stageMetadata, routingMetadata) }
//            "REDACTOR" -> {}
//            "STRUCTURE-VALIDATOR" -> {}
//            "HL7-JSON-LAKE-TRANSFORMER" -> {}
//            "LAKE-SEGMENTS-TRANSFORMER" -> {}
        }
        return null
    }

    fun mapReceiverReport(stageMetadata: JsonObject, routingMetadata: JsonObject) : HL7ReceiverReport {
        val supportingData = mutableMapOf<String,String>()
        val ingestErrors = mutableListOf<IngestError>()

        val supportingMetadata = routingMetadata["supporting_metadata"]?.asJsonObject?.asMap()
        supportingMetadata?.keys?.forEach { k -> supportingData[k] = supportingMetadata[k].toString() }

        val stageReport = stageMetadata["report"].asJsonObject
        val errors = stageReport["error_messages"]?.asJsonArray
        errors?.forEach {
            val error = it.asJsonObject
            ingestErrors.add(
                IngestError(
                    messageUuid = error["message_uuid"].asString,
                    messageIndex = error["message_index"].asLong,
                    errorMessage = error["error_message"].asString
                )
            )
        }

        val report = ReceiverReportData(
            ingestedFilePath = routingMetadata["ingested_file_path"]?.asString,
            ingestedFileTimestamp = routingMetadata["ingested_file_timestamp"]?.asString,
            ingestedFileSize = (routingMetadata["ingested_file_size"].asString ?: "0").toLong(),
            receivedFileName = routingMetadata["received_filename"].asString,
            supportingMetadata = if (supportingMetadata == null) null else supportingData,
            aggregation = if (stageReport["single_or_batch"].asString == "BATCH") AggregationType.BATCH else AggregationType.SINGLE,
            numberOfMessages = stageReport["number_of_messages"].asLong,
            numberOfMessagesNotPropagated = stageReport["number_of_messages_not_propagated"].asLong,
            errorMessages = ingestErrors
        )
        return HL7ReceiverReport(report)
    }

}