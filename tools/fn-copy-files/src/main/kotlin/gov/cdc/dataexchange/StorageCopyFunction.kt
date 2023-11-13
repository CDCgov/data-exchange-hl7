package gov.cdc.dataexchange

import com.microsoft.azure.functions.annotation.*
import com.microsoft.azure.functions.*
import gov.cdc.dataexchange.util.BlobService
import gov.cdc.dataexchange.util.PathHelper
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*

/**
 * Kotlin Azure Function with HTTP Trigger.
 * Copy file(s) in Azure Storage Containers
 * Created: 11/07/2023
 * @author QEH3@cdc.gov
 */
class StorageCopyFunction {

    companion object {
        private val logger = LoggerFactory.getLogger(StorageCopyFunction::class.java.simpleName)
    }

    @FunctionName("storage-copy")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "copy/{srcContainer}/{srcDotPath}/to/{destContainer}/{destDotPath}"
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("srcContainer") srcContainer: String,
        @BindingName("srcDotPath") srcDotPath: String,
        @BindingName("destContainer") destContainer: String,
        @BindingName("destDotPath") destDotPath: String
    ): HttpResponseMessage {
        val srcPath = PathHelper(srcDotPath).transform()
        val destPath = PathHelper(destDotPath).transform()
        logger.info("HTTP trigger processed a copy request on $srcContainer/$srcPath to $destContainer/$destPath")
        val connectionString = request.headers["connection-string"] // extract header

        // validate
        if (connectionString.isNullOrEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Missing connection string")
                .build()
        }

        // copy
        val copyResponse = BlobService.copyStorage(
            srcContainer, srcPath,
            destContainer, destPath,
            connectionString
        )

        return when (copyResponse) {
            BlobService.SUCCESS ->
                request.createResponseBuilder(HttpStatus.OK)
                    .body("Files copied successfully from location $srcContainer/$srcPath to $destContainer/$destPath")
                    .build()

            BlobService.NOT_FOUND ->
                request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("Container or directory not found at location $srcContainer/$srcPath")
                    .build()

            BlobService.FAILED_OPERATION ->
                request.createResponseBuilder(HttpStatus.EXPECTATION_FAILED)
                    .body("Copy operation failed for blob")
                    .build()

            else ->
                request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("FAIL:\n$copyResponse")
                    .build()
        }
    }
}