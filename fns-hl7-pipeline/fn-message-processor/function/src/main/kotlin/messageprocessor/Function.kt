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


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    @FunctionName("messageprocessor001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                // TODO:
                eventHubName = "eventhub006",
                connection = "EventHubConnectionString") 
                message: String?,
            context: ExecutionContext) {

        context.logger.info("message: --> " + message)

        // TODO:...
        // // 
        // // Event Hub -> receive events
        // // -------------------------------------
        // val eventArr = Gson().fromJson(message, Array<HL7Message>::class.java)
       
        // // 
        // // For each Event received
        // // -------------------------------------
        // for (event in eventArr) { 

        //     context.logger.info("event received: --> " + event)

        // } // .for 

    } // .eventHubProcessor

} // .Function

