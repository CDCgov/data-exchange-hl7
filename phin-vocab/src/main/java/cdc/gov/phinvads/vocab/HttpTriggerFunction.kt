package cdc.gov.phinvads.vocab

import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import java.util.*

/**
 * Azure Functions with HTTP Trigger.
 */
class HttpTriggerFunction {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("VocabReadFunction")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<Optional<String?>?>,
        context: ExecutionContext
    ): HttpResponseMessage {cd.
        context.logger.info("Java HTTP trigger processed a request.")
        val client = VocabClient()
        println("Calling VocabClient services")
        val valueSets = client.getAllValueSets() as List<ValueSet>?
        val vocabMap: MutableMap<StringBuilder, Any> = HashMap()
        val gson = GsonBuilder().create()
        for (e in valueSets!!) {
            val valueSetConcepts = client.getValueSetConcepts(e) as List<ValueSetConcept>?
            val key = client.getValueSetKey(e)
            vocabMap[key] = gson.toJson(valueSetConcepts)
        }
        println("END OF VocabClient services")
        return request.createResponseBuilder(HttpStatus.OK).body("VocabLists size:, " + vocabMap.size).build()
    }
}