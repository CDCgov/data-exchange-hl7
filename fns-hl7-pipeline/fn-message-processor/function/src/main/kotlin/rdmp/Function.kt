package com.example

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.Gson 
import com.azure.storage.blob.*
import com.azure.storage.blob.models.*

import com.azure.messaging.eventhubs.*

import java.util.UUID
import java.io.*

const val BLOB_CREATED = "Microsoft.Storage.BlobCreated"
const val UTF_BOM = "\uFEFF"

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    @FunctionName("messageprocessor001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                // TODO:
                eventHubName = "eventhub004",
                connection = "EventHubConnectionString") 
                message: String?,
            context: ExecutionContext) {

        context.logger.info("message: --> " + message)


    } // .eventHubProcessor

} // .Function

