package gov.cdc.ncezid.cloud.storage

import java.util.regex.Pattern

data class CloudFile(
    val bucket: String,
    val fileName: String,
    val metadata: Map<String, String>,
    val content: String
) {

    /**
     * Simple row count based on line termination characters. Counts either \n\r, \r\n, \n, or \r
     * Executed lazily.
     */
    val rowCount: Long by lazy {
        Pattern.compile("(\n\r)|(\r\n)|(\n)|(\r)").matcher(content).results().count()
    }

    override fun toString(): String {
        return """
            |CloudFile(
            |  bucket='$bucket', 
            |  fileName='$fileName', 
            |  metadata=$metadata, 
            |  content='${content.take(100)}...${content.drop(100)}'
            |)""".trimMargin()
    }
}
