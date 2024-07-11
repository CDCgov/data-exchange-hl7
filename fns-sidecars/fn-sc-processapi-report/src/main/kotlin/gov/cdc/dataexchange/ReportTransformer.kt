package gov.cdc.dataexchange

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.reports.*
import gov.cdc.dex.util.JsonHelper.gson
import gov.cdc.hl7.RedactInfo
import java.lang.reflect.Type

class ReportTransformer {
    private inline fun <reified T> genericType(): Type = object: TypeToken<T>() {}.type

    fun mapDataToBaseReport(record: String) : PSBaseReport {
        val inputEvent = JsonParser.parseString(record).asJsonObject
        val messageMetadata = inputEvent["message_metadata"]?.asJsonObject
        val routingMetadata = inputEvent["routing_metadata"].asJsonObject
        val stageMetadata = inputEvent["stage"].asJsonObject
        val summary = inputEvent["summary"]?.asJsonObject

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

    private fun mapMessageMetadata(messageMetadata: JsonObject?) : MessageMetadata? {
        if (messageMetadata == null) return null
        return MessageMetadata(
            messageUuid = messageMetadata["message_uuid"].asString,
            messageHash = messageMetadata["message_hash"].asString,
            aggregation = if (messageMetadata["single_or_batch"].asString == "BATCH") AggregationType.BATCH else AggregationType.SINGLE,
            messageIndex = messageMetadata["message_index"].asInt

        )
    }

    private fun mapReceiverStageInfo(stageMetadata: JsonObject) : StageInfo {
        val report = stageMetadata["report"].asJsonObject
        var status = StageStatus.SUCCESS
        val issues = mutableListOf<Issue>()
        if (report["number_of_messages_not_propagated"].asInt > 0) {
            if (report["number_of_messages_not_propagated"].asInt == report["number_of_messages"].asInt) {
                status = StageStatus.FAILURE
                report["error_messages"]?.asJsonArray?.forEach {
                    issues.add(Issue(level = IssueLevel.ERROR, message = it.asJsonObject["error_message"].asString))
                }
            }
        }
        return StageInfo(
            stage = stageMetadata["stage_name"].asString,
            version = stageMetadata["stage_version"].asString,
            status = status,
            issues = issues,
            startProcessingTime = stageMetadata["start_processing_time"].asString,
            endProcessingTime = stageMetadata["end_processing_time"].asString
        )

    }

    private fun mapStageInfo(stageMetadata: JsonObject, summaryInfo: JsonObject?) : StageInfo {
        if (summaryInfo == null) { return mapReceiverStageInfo(stageMetadata) }
        val problem = summaryInfo.get("problem")
        val issues = if (problem == null || problem.isJsonNull) null else listOf(
            Issue(
                level = IssueLevel.ERROR,
                message = problem.asJsonObject["error_message"].asString
            )
        )
        val status = if (issues != null) "FAILURE" else stageMetadata["status"]?.asString

        return StageInfo(
            stage = stageMetadata["stage_name"].asString,
            version = stageMetadata["stage_version"].asString,
            status = if (status == "SUCCESS") StageStatus.SUCCESS else StageStatus.FAILURE,
            issues = if (problem == null || problem.isJsonNull) null else listOf(
                Issue(
                level = IssueLevel.ERROR,
                message = problem.asJsonObject["error_message"].asString
            )
            ),
            startProcessingTime = stageMetadata["start_processing_time"].asString,
            endProcessingTime = stageMetadata["end_processing_time"].asString
        )
    }

    private fun mapStageContent(stageMetadata: JsonObject, routingMetadata: JsonObject) : StageContent? {
        val stage = stageMetadata["stage_name"].asString
        when (stage) {
            "RECEIVER" -> { return mapReceiverReport(stageMetadata, routingMetadata) }
            "REDACTOR" -> { return mapRedactorReport(stageMetadata) }
            "STRUCTURE-VALIDATOR" -> { return mapStructureValidatorReport(stageMetadata) }
            "HL7-JSON-LAKE-TRANSFORMER" -> {  return HL7JsonLakeReport(configs = getConfigs(stageMetadata)) }
            "LAKE-SEGMENTS-TRANSFORMER" -> { return HL7LakeSegmentsReport(configs = getConfigs(stageMetadata)) }
        }
        return null
    }

    private fun mapReceiverReport(stageMetadata: JsonObject, routingMetadata: JsonObject) : HL7ReceiverReport {
        val ingestErrors = mutableListOf<IngestError>()
        val supportingMetadata = routingMetadata["supporting_metadata"]?.asJsonObject
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

        return HL7ReceiverReport(
            report = ReceiverReportData(
                ingestedFilePath = routingMetadata["ingested_file_path"]?.asString,
                ingestedFileTimestamp = routingMetadata["ingested_file_timestamp"]?.asString,
                ingestedFileSize = (routingMetadata["ingested_file_size"].asString ?: "0").toLong(),
                receivedFileName = routingMetadata["received_filename"].asString,
                supportingMetadata = convertJsonToStringMap(supportingMetadata),
                aggregation = if (stageReport["single_or_batch"].asString == "BATCH") AggregationType.BATCH else AggregationType.SINGLE,
                numberOfMessages = stageReport["number_of_messages"].asLong,
                numberOfMessagesNotPropagated = stageReport["number_of_messages_not_propagated"].asLong,
                errorMessages = ingestErrors
            )
        )
    }

    private fun mapRedactorReport(stageMetadata: JsonObject) : HL7RedactorReport {
        val configs = getConfigs(stageMetadata)
        val stageReport = stageMetadata["report"].asJsonObject
        val entries = stageReport["entries"].asJsonArray
        val redactInfoListType = genericType<List<RedactInfo>>()
        val newEntries : List<RedactInfo> = gson.fromJson(entries, redactInfoListType)
        return HL7RedactorReport(
            RedactorReportData(
                entries = newEntries
            ),
            configs = configs
        )
    }

    private fun mapStructureValidatorReport(stageMetadata: JsonObject) : HL7StructureValidatorReport {
        val stageReport = stageMetadata["report"].asJsonObject
        val configs = getConfigs(stageMetadata)
        return HL7StructureValidatorReport(
            report = stageReport,
            configs = configs
        )

    }

    private fun getConfigs(stageMetadata: JsonObject) : List<String> {
        val configs = stageMetadata["configs"].asJsonArray
        val listType = genericType<List<String>>()
        return gson.fromJson(configs, listType)
    }

    private fun convertJsonToStringMap(jsonObject: JsonObject?) : Map<String, String>? {
        if (jsonObject == null) return null
        val mapType = genericType<Map<String, String>>()
        return gson.fromJson(jsonObject, mapType)
    }

}