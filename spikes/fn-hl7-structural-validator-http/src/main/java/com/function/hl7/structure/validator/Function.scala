package com.function.hl7.structure.validator;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

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
    @FunctionName("HttpExample")
    def run(
        @HttpTrigger(
          name = "req",
          methods = Array(HttpMethod.GET, HttpMethod.POST),
          authLevel = AuthorizationLevel.ANONYMOUS) request: HttpRequestMessage[Optional[String]],
        context: ExecutionContext): HttpResponseMessage = {
        context.getLogger.info("Scala HTTP trigger processed a request.")

        request.getHttpMethod() match {
          case HttpMethod.POST => {

            request.getBody().isPresent match {
              case true => {

                // ASYNC
                val validator = StructureValidatorConc()

                validator.reportJSON(request.getBody().get) match {

                    case Success( report ) => {
                        request.createResponseBuilder(HttpStatus.OK).body(report).build
                    } // .Success 

                    case Failure( err ) => {
                      request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(err.getMessage).build

                    } // .Failure

                } // .Try  

              } // .true

              case _ => request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("empty request body").build
            } // .match

          } // .post

          case _  => request.createResponseBuilder(HttpStatus.OK).body("Use POST with body text HL7 message").build
          
        } // .request
        
    } // .HttpTrigger
}
