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

/**
 * Azure Functions with HTTP Trigger.
 */
class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    def run(
        @HttpTrigger(
          name = "req",
          methods = Array(HttpMethod.GET, HttpMethod.POST),
          authLevel = AuthorizationLevel.ANONYMOUS) request: HttpRequestMessage[Optional[String]],
        context: ExecutionContext): HttpResponseMessage = {
        context.getLogger.info("Scala HTTP trigger processed a request.")
        request.createResponseBuilder(HttpStatus.OK).body("fn Scala").build
    } // .HttpTrigger
}
