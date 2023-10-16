package gov.cdc.dataexchange.client

import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.CosmosException
import com.azure.cosmos.implementation.HttpConstants
import com.azure.cosmos.models.CosmosBulkExecutionOptions
import com.azure.cosmos.models.CosmosBulkItemResponse
import com.azure.cosmos.models.CosmosBulkOperationResponse
import com.azure.cosmos.models.CosmosItemOperation
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitFailureHandler
import reactor.core.publisher.Sinks.EmitResult
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Semaphore


class BulkWriter(private val cosmosAsyncContainer: CosmosAsyncContainer) {
    private val bulkInputEmitter = Sinks.many().unicast().onBackpressureBuffer<CosmosItemOperation>()
    private val cpuCount = Runtime.getRuntime().availableProcessors()

    //Max items to be buffered to avoid out of memory error
    private val semaphore = Semaphore(1024 * 167 / cpuCount)
    private val emitFailureHandler =
        EmitFailureHandler { signalType: SignalType?, emitResult: EmitResult ->
            if (emitResult == EmitResult.FAIL_NON_SERIALIZED) {
                logger.debug(
                    "emitFailureHandler - Signal: [{}], Result: [{}]",
                    signalType,
                    emitResult
                )
                return@EmitFailureHandler true
            } else {
                logger.error(
                    "emitFailureHandler - Signal: [{}], Result: [{}]",
                    signalType,
                    emitResult
                )
                return@EmitFailureHandler false
            }
        }

    fun scheduleWrites(cosmosItemOperation: CosmosItemOperation) {
        while (!semaphore.tryAcquire()) {
            logger.info("Unable to acquire permit")
        }
        logger.info("Acquired permit")
        scheduleInternalWrites(cosmosItemOperation)
    }

    private fun scheduleInternalWrites(cosmosItemOperation: CosmosItemOperation) {
        bulkInputEmitter.emitNext(cosmosItemOperation, emitFailureHandler)
    }

    @JvmOverloads
    fun execute(bulkOptions: CosmosBulkExecutionOptions? = null): Flux<CosmosBulkOperationResponse<*>> {
        var bulkOptions = bulkOptions
        if (bulkOptions == null) {
            bulkOptions = CosmosBulkExecutionOptions()
        }
        return cosmosAsyncContainer
            .executeBulkOperations<Any>(
                bulkInputEmitter.asFlux(),
                bulkOptions
            )
            .publishOn(Schedulers.boundedElastic()).map { bulkOperationResponse: CosmosBulkOperationResponse<Any> ->
                processBulkOperationResponse(
                    bulkOperationResponse.response,
                    bulkOperationResponse.operation,
                    bulkOperationResponse.exception
                )
                semaphore.release()
                bulkOperationResponse
            }
    }

    private fun processBulkOperationResponse(
        itemResponse: CosmosBulkItemResponse,
        itemOperation: CosmosItemOperation,
        exception: Exception?
    ) {
        if (exception != null) {
            handleException(itemOperation, exception)
        } else {
            processResponseCode(itemResponse, itemOperation)
        }
    }

    private fun processResponseCode(
        itemResponse: CosmosBulkItemResponse,
        itemOperation: CosmosItemOperation
    ) {
        if (itemResponse.isSuccessStatusCode) {
            logger.info(
                "The operation for Item ID: [{}]  Item PartitionKey Value: [{}] completed successfully " +
                        "with a response status code: [{}]",
                itemOperation.id,
                itemOperation.partitionKeyValue,
                itemResponse.statusCode
            )
        } else if (shouldRetry(itemResponse.statusCode)) {
            logger.info(
                "The operation for Item ID: [{}]  Item PartitionKey Value: [{}] will be retried",
                itemOperation.id,
                itemOperation.partitionKeyValue
            )
            //re-scheduling
            scheduleWrites(itemOperation)
        } else {
            logger.info(
                "The operation for Item ID: [{}]  Item PartitionKey Value: [{}] did not complete successfully " +
                        "with a response status code: [{}]",
                itemOperation.id,
                itemOperation.partitionKeyValue,
                itemResponse.statusCode
            )
        }
    }

    private fun handleException(itemOperation: CosmosItemOperation, exception: Exception) {
        if (exception !is CosmosException) {
            logger.info(
                "The operation for Item ID: [{}]  Item PartitionKey Value: [{}] encountered an unexpected failure",
                itemOperation.id,
                itemOperation.partitionKeyValue
            )
        } else {
            if (shouldRetry(exception.statusCode)) {
                logger.info(
                    "The operation for Item ID: [{}]  Item PartitionKey Value: [{}] will be retried",
                    itemOperation.id,
                    itemOperation.partitionKeyValue
                )

                //re-scheduling
                scheduleWrites(itemOperation)
            }
        }
    }

    private fun shouldRetry(statusCode: Int): Boolean {
        return statusCode == HttpConstants.StatusCodes.REQUEST_TIMEOUT ||
                statusCode == HttpConstants.StatusCodes.TOO_MANY_REQUESTS
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BulkWriter::class.java)
    }
}

 