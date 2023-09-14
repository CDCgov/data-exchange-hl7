package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.OutputBinding
import gov.cdc.dex.azure.EventHubMetadata
import java.util.logging.Level
import java.util.logging.Logger

class Function {
    fun eventHubProcessor(
        messages: List<String?>,
        eventHubMD:List<EventHubMetadata>,
        outOk: OutputBinding<List<String>>,
        outErr: OutputBinding<List<String>>,
        outCosmos: OutputBinding<List<JsonObject>>,
        context:ExecutionContext
    ): List<JsonObject> {
        return listOf()
    }
}

object context : ExecutionContext {
    override fun getLogger() = Logger.getAnonymousLogger().apply {
        this.level = Level.parse(System.getenv("LOG-LEVEL"))}
    override fun getInvocationId() = "fn-lake-segs-transformer_id"
    override fun getFunctionName() = "fn-lake-segs-transformer"
}

class OutBinding<T>(private var value:T?=null): OutputBinding<T> {
    override fun getValue(): T? = value
    override fun setValue(v:T?): Unit  { value = v }
}

