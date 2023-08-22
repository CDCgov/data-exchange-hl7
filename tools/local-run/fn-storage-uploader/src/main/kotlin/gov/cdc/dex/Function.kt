package gov.cdc.dex

import java.util.*
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import kotlin.io.path.Path
import kotlin.io.path.writeText
import com.google.gson.JsonObject

class Function {
    companion object {
        val containerPath: String = System.getenv("BlobIngestConnectionString")
        val containerName:String = System.getenv("BlobIngestContainerName")
    }

    @FunctionName("HttpTrigger-Java")
    fun run(
            @HttpTrigger(
                    name = "req",
                    methods = [HttpMethod.POST],
                    authLevel = AuthorizationLevel.FUNCTION) request: HttpRequestMessage<Optional<String>>,
            context: ExecutionContext): HttpResponseMessage {

        val msg = if (request.body.isPresent) request.body.get()
        else
            return  request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Please send an HL7 v.2.x message in the body of the request")
                .build()

        val msgType = request.headers["x-tp-message_type"]
        if ( msgType.isNullOrEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("HTTP Header \"x-tp-message-type\" with value \"CASE\" or \"ELR\" is required")
                .build()
        }

        val route = request.headers["x-tp-route"]
        if ( route.isNullOrEmpty() && msgType == "ELR") {
           return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
               .body("HTTP Header \"x-tp-route\" is required if  \"x-tp-message-type\" is \"ELR\"")
               .build()
        }

        val originalFileName = request.headers["x-tp-original_file_name"] ?: "$msgType-${UUID.randomUUID()}"

        // extract file name and extension
        val extIndex = originalFileName.lastIndexOf('.')
        val fileNameOnly = if (extIndex != -1)
            originalFileName.substring(0, extIndex)
        else originalFileName
        val fullFileName = if (extIndex == -1) "$originalFileName.txt" else originalFileName

        val metadata = JsonObject().apply {
            addProperty("message_type", msgType)
            addProperty("original_file_name",fullFileName)
            addProperty(
                "system_provider",
                request.headers["x-tp-system_provider"] ?: "LOCAL"
            )
            addProperty(
                "reporting_jurisdiction",
                request.headers["x-tp-reporting_jurisdiction"] ?: "00"
            )
            if (msgType == "ELR") {
                addProperty("route", route)
            }
        }
        val props = JsonObject().apply {
            addProperty("blob_size", msg.length)
            add("metadata", metadata)
        }

        // write message and properties
        Path("$containerPath/$containerName/$fullFileName").writeText(msg)
        Path("$containerPath/$containerName/$fileNameOnly.properties").writeText(props.toString())

        return request.createResponseBuilder(HttpStatus.OK)
            .body("Message uploaded successfully")
            .build()
    }
}


