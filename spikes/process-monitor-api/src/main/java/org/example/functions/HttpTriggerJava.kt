package org.example.functions

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import java.sql.DriverManager
import java.util.*

/**
 * Azure Functions with HTTP Trigger.
 */
class HttpTriggerJava {
    /**
     * This function listens at endpoint "/api/HttpTriggerJava". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTriggerJava
     * 2. curl {your host}/api/HttpTriggerJava?name=HTTP%20Query
     */
    @FunctionName("HttpTriggerJava")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<Optional<String?>>,
        context: ExecutionContext
    ): HttpResponseMessage {
        context.logger.info("Java HTTP trigger processed a request.")

        // Parse query parameter
        val query = request.queryParameters["messageId"]
        val messageId = request.body.orElse(query)

        val server = System.getenv("DATABRICKS_SERVER_HOSTNAME")
        val httpPath = System.getenv("DATABRICKS_HTTP_PATH")
        val dbxToken = System.getenv("DATABRICKS_TOKEN")
        val databricksURL = "jdbc:databricks://$server:443/default;transportMode=http;ssl=1;httpPath=$httpPath;AuthMech=3;UID=token;PWD=$dbxToken"

        val conn = DriverManager.getConnection((databricksURL))
        val sqlQuery = """SELECT summary.current_status as current_status
                        ,status
                        ,lake_metadata
                    FROM OCIO_DEX_DEV.hl7_validation_report_silver 
                    WHERE MESSAGE_UUID = ?
                    AND process_name = 'STRUCTURE-VALIDATOR'"""
        val preparedStatement = conn.prepareStatement(sqlQuery)
        preparedStatement.setString(1,messageId)
        val result = preparedStatement.executeQuery()


        var rowCount = 0
        val jsonList = mutableListOf<Map<String,Any>>()
        while (result.next()) {
            val mapData = mapOf(
                "current_status" to result.getString("current_status"),
                "status" to result.getString("status")
            )
            rowCount += 1
            jsonList.add(mapData)

        }
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val jsonOutput = gson.toJson(jsonList)

        result.close()
        preparedStatement.close()
        conn.close()
        println(rowCount)
        return if (rowCount > 0) {
            request.createResponseBuilder(HttpStatus.OK).body(jsonOutput).build()

        } else {
            request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Please enter a valid ID: $messageId does not exist").build()
        }
    }
}
