package gov.cdc.dex.hl7


import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.DexEventPayload
import java.util.logging.Level
import java.util.logging.Logger

class Function {
    fun eventHubProcessor( message: List<String?>, eventHubMD:List<EventHubMetadata>,  context: ExecutionContext): DexEventPayload? {
        return null
    }
}

object context : ExecutionContext {
    override fun getLogger() = Logger.getAnonymousLogger().apply {
        this.level = Level.parse(System.getenv("LOG-LEVEL"))}

    override fun getInvocationId() = "fn-receiver-debatcher_id"
    override fun getFunctionName() = "fn-receiver-debatcher"
}

