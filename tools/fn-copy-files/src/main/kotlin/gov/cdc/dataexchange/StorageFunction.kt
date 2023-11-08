package gov.cdc.dataexchange

import com.microsoft.azure.functions.annotation.*
import com.microsoft.azure.functions.*
import gov.cdc.dataexchange.util.BlobService
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Kotlin Azure Function with HTTP Trigger.
 * Copy file(s) in Azure Storage Containers
 * Created: 11/07/2023
 * @author QEH3@cdc.gov
 */
class StorageFunction {

    companion object {
        private val logger = LoggerFactory.getLogger(StorageFunction::class.java.simpleName)
    }

    @FunctionName("file-copy")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "file-copy/{srcContainer}/{dotPath}/{filename}"
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("srcContainer") srcContainer: String,
        @BindingName("dotPath") dotPath: String,
        @BindingName("filename") filename: String
    ): HttpResponseMessage {
        var srcPath = if (dotPath != "ROOT") dotPath else ""
        if (srcPath.isNotBlank()) {
            srcPath = srcPath.replace('.', '/')
        }
        logger.info("FCOPY::HTTP trigger processed a copy request on $srcContainer/$srcPath/$filename.")

        // Extract headers
        val destContainer = request.headers["destination-container"]
        val destPath = request.headers["destination-folder-path"] ?: ""
        val connectionString = request.headers["connection-string"]
        val srcConnectionString = request.headers["src-connection-string"]
        val destConnectionString = request.headers["dest-connection-string"]
        val n = request.headers["n"]?.toIntOrNull()

        // validate headers
        when {
            connectionString.isNullOrEmpty() && (srcConnectionString.isNullOrEmpty() || destConnectionString.isNullOrEmpty()) ->
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Missing connection strings")
                    .build()

            !connectionString.isNullOrEmpty() && (!srcConnectionString.isNullOrEmpty() || !destConnectionString.isNullOrEmpty()) ->
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Use either 'connection-string' or both 'src-connection-string' and 'dest-connection-string'")
                    .build()

            (!srcConnectionString.isNullOrEmpty() && destConnectionString.isNullOrEmpty())
                    || (srcConnectionString.isNullOrEmpty() && !destConnectionString.isNullOrEmpty()) ->
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Both 'src-connection-string' and 'dest-connection-string' must be provided")
                    .build()

            destContainer.isNullOrEmpty() ->
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing 'destination-container' header").build()
        }

        // copy
        val copyResult = BlobService.copyFile(
            srcContainer,
            destContainer!!,
            srcPath,
            destPath,
            filename,
            connectionString ?: srcConnectionString!!,
            connectionString ?: destConnectionString!!,
            n
        )

        // resolve response
        return when (copyResult) {
            BlobService.SUCCESS ->
                request.createResponseBuilder(HttpStatus.OK)
                    .body("File(s) copied successfully from location $srcContainer/$srcPath/$filename to $destContainer/$destPath")
                    .build()

            BlobService.NOT_FOUND ->
                request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("file not found at location $srcContainer/$srcPath/$filename")
                    .build()

            BlobService.FAILED_OPERATION ->
                request.createResponseBuilder(HttpStatus.EXPECTATION_FAILED).body("Copy operation failed for blob").build()

            else ->
                request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Copy operation failed for blob\n$copyResult").build()
        }
    }
}