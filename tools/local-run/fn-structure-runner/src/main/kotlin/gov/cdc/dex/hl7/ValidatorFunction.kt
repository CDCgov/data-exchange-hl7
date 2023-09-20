package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import java.util.logging.Level
import java.util.logging.Logger
import com.google.gson.JsonObject
import com.microsoft.azure.functions.OutputBinding
import com.microsoft.azure.functions.annotation.*

class ValidatorFunction {
    fun eventHubProcessor(
        message: List<String?>,
        eventHubMD:List<EventHubMetadata>,
        context:ExecutionContext,
        outOk: OutputBinding<List<String>>,
        outErr: OutputBinding<List<String>>,
        outCosmos: OutputBinding<List<JsonObject>>
    ): JsonObject {
        return JsonObject()
    }
}

object context : ExecutionContext {
    override fun getLogger() = Logger.getAnonymousLogger().apply {
        this.level = Level.parse(System.getenv("LOG-LEVEL"))}
    override fun getInvocationId() = "fn-structure-validator_id"
    override fun getFunctionName() = "fn-structure-validator"
}

class OutBinding<T>(private var value:T?=null): OutputBinding<T> {
    override fun getValue(): T? = value
    override fun setValue(v:T?): Unit  { value = v }
}


