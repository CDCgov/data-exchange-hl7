package com.function.hl7.structure.validator;

// import com.microsoft.azure.functions.ExecutionContext;
// import com.microsoft.azure.functions.HttpMethod;
// import com.microsoft.azure.functions.HttpRequestMessage;
// import com.microsoft.azure.functions.HttpResponseMessage;
// import com.microsoft.azure.functions.HttpStatus;
// import com.microsoft.azure.functions.annotation.AuthorizationLevel;
// import com.microsoft.azure.functions.annotation.FunctionName;
// import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import java.util.Optional;

import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import scala.concurrent._

import cdc.xlr.structurevalidator._;

/**
 * Azure Functions with HTTP Trigger.
 */
class Function {
    /**
     * This function listens at endpoint "/api/HttpExample"
     */
    @FunctionName("structuralvalidator001")
    def run(
        @EventHubTrigger(
          name = "msg",
          eventHubName = "eventhub002",
          connection = "EventHubConnectionString") 
          message: String,
          context: ExecutionContext): Unit = {

          context.getLogger.info("Scala event hub triggered a request.")
          context.getLogger.info("message: -> " + message)
        
    } // .EventHubTrigger
}
