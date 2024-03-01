package gov.cdc.dex.azure.ProcessingStatus



import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.slf4j.LoggerFactory

class PSClientUtility {

    companion object {
        private val logger = LoggerFactory.getLogger(PSClientUtility::class.java.simpleName)
        val gson: Gson = GsonBuilder().serializeNulls().create()
    }

    data class Trace(@SerializedName("trace_id") val traceId:String,
                     @SerializedName("span_id") val spanId:String )

    fun sendTraceToProcessingStatus(
        psURL: String,
        traceId: String,
        parentSpanId: String,
        span: String,
        stageName : String
    ): String {
        logger.info("DEX:calling sendTraceToProcessingStatus")
        val url = "$psURL/api/trace/$span/$traceId/$parentSpanId?stageName=$stageName"
        logger.info(url)

        val (_, response, result) = url.httpPut().responseString()
        return when(result){
            is Result.Success -> {
                val payload  = result.value
                val trace = gson.fromJson(payload, Trace::class.java)
                trace.spanId  //returning spanId
            }

            is Result.Failure -> {
                val error = result.error
                logger.error("Failure in sending trace to Processing status API:${error.message}")
                ""
            }
        }
    }

     fun stopTrace( psURL: String,
                           traceId: String,
                           childSpanId: String,
                           stageName : String
                          ) {
        val url = "$psURL/api/trace/stopSpan/$traceId/$childSpanId?stageName=$stageName&spanMark=stop"
        url.httpPut().responseString()
        }


}
