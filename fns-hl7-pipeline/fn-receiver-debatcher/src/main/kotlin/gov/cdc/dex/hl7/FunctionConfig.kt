package gov.cdc.dex.hl7

import com.azure.storage.internal.avro.implementation.schema.primitive.AvroNullSchema.Null
import gov.cdc.dex.azure.DedicatedEventHubSender
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.util.PathUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import java.lang.NullPointerException

class FunctionConfig {
    val azBlobProxy: AzureBlobProxy
    val evHubSenderOut: DedicatedEventHubSender
    val evHubSenderReports: DedicatedEventHubSender
    var eventCodes : Map<String, Map<String, String>>
    val logger = LoggerFactory.getLogger(this.javaClass.simpleName)
    val blobIngestContName: String = try {
        System.getenv("BlobIngestContainerName")
    } catch (e: NullPointerException) {
        logger.error("FATAL: Missing environment variable BlobIngestContainerName")
        throw e
    }
    val evHubSendName: String = try {
        System.getenv("EventHubSendName")
    } catch(e: NullPointerException) {
        logger.error("FATAL: Missing environment variable EventHubSendName")
        throw e
    }
    val evReportsHubName: String = try {
        System.getenv("EventReportsHubName")
    } catch (e: NullPointerException) {
        logger.error ("FATAL: Missing environment variable EventReportsHubName")
        throw e
    }
    val psURL = System.getenv("ProcessingStatusBaseURL")


    init {
         //Init Azure Storage connection
        val ingestBlobConnStr = try {
            System.getenv("BlobIngestConnectionString")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable BlobIngestConnectionString")
            throw e
        }
        azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)
        val evHubConnStr = try {
            System.getenv("EventHubConnectionString")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable EventHubConnectionString")
            throw e
        }
        evHubSenderOut = DedicatedEventHubSender(evHubConnStr, evHubSendName)
        evHubSenderReports = DedicatedEventHubSender(evHubConnStr, evReportsHubName)

        //Load Event Codes
        eventCodes = loadEventCodes()
    }

    private fun loadEventCodes() : Map<String, Map<String, String>> {
        val dir = PathUtils().getResourcePath("event_codes")
        val codesMap = mutableMapOf<String, Map<String,String>>()
        Files.walk(dir).filter { it.isRegularFile() && it.extension.lowercase() == "csv"}.forEach { f ->
            run {
                val parser = CSVParser.parse(
                    f, Charsets.UTF_8,
                    CSVFormat.DEFAULT.builder().setHeader()
                        .setSkipHeaderRecord(true).setAllowMissingColumnNames(true).build()
                )

                for (row: CSVRecord in parser) {
                    val code = row["Condition Code"]
                    val name = Pair("condition_name", row["Condition Name"].normalize())
                    val category = Pair("category", row["Category"].normalize())
                    val program = Pair("program", row["Program"].normalize())
                    codesMap[code] = mapOf(name, category, program)
                }
                parser.close()
            }
        }
        return codesMap.toMap()
    }
}