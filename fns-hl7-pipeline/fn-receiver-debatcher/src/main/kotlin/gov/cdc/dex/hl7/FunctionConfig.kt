package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.util.PathUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

class FunctionConfig {

    val azBlobProxy: AzureBlobProxy
    var eventCodes : Map<String, Map<String, String>>
    val blobIngestContName = System.getenv("BlobIngestContainerName")
    val functionVersion = System.getenv("FN_VERSION")?.toString() ?: "Unknown"

    init {

         //Init Azure Storage connection
         val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")
         azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)

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