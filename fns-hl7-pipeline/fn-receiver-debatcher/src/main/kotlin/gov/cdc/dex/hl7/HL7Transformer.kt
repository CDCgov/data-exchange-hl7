package gov.cdc.dex.hl7

import com.azure.core.util.BinaryData
import gov.cdc.hl7.HL7StaticParser
import java.util.*
import java.util.Base64.getDecoder
import java.util.Base64.getEncoder

/**
 * Util provides transformations to HL7 segments.
 * @param hl7Message The HL7 message containing potential binary data.
 * @author QEH3@cdc.gov
 */
class HL7Transformer(
    private val hl7Message: List<String>,
    private val dirPath: String? = UUID.randomUUID().toString(),
    private val fnConfig: FunctionConfig
) {

    companion object {
        const val BINARY_PATH = "OBX[@2='ED']-5[1].5" // HL7-PET path to get just base64 binary data
        const val SEGMENT_PATH = "OBX[@2='ED']" // HL7-PET path to get entire embedded data segment
        const val FILE_TYPE_PATH = "OBX[@2='ED']-5[1].3" // HL7-PET path to get embedded data type
    }

    private fun hasEmbeddedData() : Boolean = HL7StaticParser.getFirstValue(hl7Message.joinToString("\n"), BINARY_PATH).isDefined

    private fun getEmbeddedDataSegment(): String = HL7StaticParser.getFirstValue(hl7Message.joinToString("\n"), SEGMENT_PATH).get()

    /**
     * Get line number of the embedded data segment.
     * @return line number
     */
    private fun getLineNumber(): Int {
        val dataSegment = getEmbeddedDataSegment()
        for ((index, seg) in hl7Message.withIndex()) {
            if (seg == dataSegment) return index + 1
        }
        return 0
    }

    /**
     * Removes binary data from an HL7 segment of the message, if it contains it.  Then uploads data to
     * Azure Blob Storage, and replaces the binary data with the URL to the blob.  Returns
     * @return The modified HL7 message base64 encoded String
     */
    fun removeBinaryToBase64(): String {
        return if (hasEmbeddedData()) {
            getEncoder().encodeToString(removeBinary().joinToString("\n").toByteArray(Charsets.UTF_8))
        } else getEncoder().encodeToString(hl7Message.joinToString("\n").toByteArray(Charsets.UTF_8))
    }
    fun removeBinary(): List<String> {
        val binaryData: BinaryData = extractBinaryData() ?: return hl7Message
        val blobUrl = uploadBinaryDataToBlob(binaryData) ?: return hl7Message
        val newSeg = replaceBinaryDataWithUrl(getEmbeddedDataSegment(), blobUrl)
        return transformHl7Message(newSeg)
    }

    /**
     * Extracts binary data in a given hl7 message
     * @return BinaryData or null if not found
     */
    private fun extractBinaryData(): BinaryData? {
        val binarySeg = HL7StaticParser.getValue(hl7Message.joinToString("\n"), BINARY_PATH)
        return if (binarySeg.isDefined) {
            val base64Data = binarySeg.get().firstOrNull()?.firstOrNull()
            if (!base64Data.isNullOrEmpty()) {
                BinaryData.fromBytes(getDecoder().decode(base64Data))
            } else null
        } else null
    }

    /**
     * Upload binary data to a blob in a given folder path.
     * @return blob url string
     */
    private fun uploadBinaryDataToBlob(data: BinaryData): String? {
        return try {
            var extension = HL7StaticParser.getValue(hl7Message.joinToString("\n"), FILE_TYPE_PATH)
                .get().firstOrNull()?.firstOrNull() ?: ""
            extension = if (extension.isNotBlank()) ".$extension" else extension
            val blobName = "$dirPath/${getLineNumber()}$extension"
            val attachmentBlob = fnConfig.attachmentBlobProxy.getBlobClient(blobName)
            attachmentBlob.upload(data)
            attachmentBlob.blobUrl
        } catch (e: Exception) { null }
    }

    /**
     * Transform HL7 by replacing binary data in segment with blob url.
     * @return transformed segment string
     */
    private fun replaceBinaryDataWithUrl(hl7Segment: String, url: String): String {
        val base64StartIndex = hl7Segment.indexOf("^Base64^") + "^Base64^".length
        if (base64StartIndex >= "^Base64^".length) {
            val base64EndIndex = hl7Segment.indexOf("\r", base64StartIndex).takeIf { it > 0 } ?: hl7Segment.length
            return hl7Segment.replaceRange(base64StartIndex, base64EndIndex, url)
        }
        return hl7Segment
    }

    /**
     * Updates new segment found in HL7 at line number with transformed segment
     * @return transformed HL7 message
     */
    private fun transformHl7Message(newSeg: String) : List<String> {
        val segments = hl7Message.toMutableList()
        val lineIndex = getLineNumber() - 1
        if (lineIndex in segments.indices) {
            segments[lineIndex] = newSeg
        } else {
            return hl7Message
        }
        return segments
    }

}