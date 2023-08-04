package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import java.util.logging.Level
import java.util.logging.Logger

class Function {
    fun eventHubCASEProcessor( message: List<String?>, eventHubMD:List<EventHubMetadata>,  context: ExecutionContext): List<JsonObject> {
        return ArrayList()
    }
}

object context : ExecutionContext {
    override fun getLogger() = Logger.getAnonymousLogger().apply {
        this.level = Level.parse(System.getenv("LOG-LEVEL"))}
    override fun getInvocationId() = "fn-lake-segs-transformer_id"
    override fun getFunctionName() = "fn-lake-segs-transformer"
}

