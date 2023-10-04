package gov.cdc.dex

import com.google.gson.*
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import java.util.*


data class Meta(
    var message_type: String,
    var system_provider:String
)

data class Msg(
    var id: String,
    var message_uuid: String,
    var metadata: Meta
)

class Function {
    companion object {
        val gson = GsonBuilder().serializeNulls().create()!!
    }
    @FunctionName("itemsByContainer")
    fun cosmosInput(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "{cont}"
        )
        request: HttpRequestMessage<Optional<String>>,
        @CosmosDBInput(name="cosmosdevpublic",
            connection = "CosmosDBConnectionString",
            containerName = "{cont}",
            databaseName = "*** change me ***",
            sqlQuery = "SELECT * FROM Items c ")
        items: List<JsonObject>?
    ): HttpResponseMessage? {
        return if (items != null) {
            request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .header("Access-Control-Allow-Origin", "*")
                    .body(items).build()
        } else {
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Did not find expected item in ItemsCollectionIn").build()
        }
    }

    @FunctionName("testCosmosDBOutput")
    fun cosmosOutput(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>,
        @CosmosDBOutput(name="cosmosdevpublic",
            connection = "CosmosDBConnectionString",
            containerName = "hl7-json-test", createIfNotExists = true,
            partitionKey = "/message_uuid", databaseName = "hl7-events")
        cosmosOutput: OutputBinding<String>): HttpResponseMessage {

        val a = mutableListOf<JsonObject>().apply {
            add( gson.toJsonTree(Msg(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                Meta("CASE", "POSTMAN"))) as JsonObject)
            add( gson.toJsonTree(Msg(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                Meta("CASE", "LOCAL"))) as JsonObject)
            add( gson.toJsonTree(Msg(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                Meta("ELR", "POSTMAN"))) as JsonObject)
        }
        cosmosOutput.value = a.toString()
        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "text/plain")
            .body("Posted ...")
            .build()
    }
}
