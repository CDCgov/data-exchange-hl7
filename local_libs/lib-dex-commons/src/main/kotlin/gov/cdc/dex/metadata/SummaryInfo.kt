package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName


data class SummaryInfo(
    @SerializedName("current_status") val currentStatus: String,
    var problem: Problem? = null
)

data class Problem(
    @SerializedName("process_name") val processName: String,
    @SerializedName("exception_class") var exceptionClass: String? = null,
    @SerializedName("stacktrace") var stackTrace: String? = null,
    @SerializedName("error_message") var errorMessage: String?,
    @SerializedName("should_retry") val shouldRetry: Boolean,
    @SerializedName("retry_count") val retryCount: Int,
    @SerializedName("max_retries") val maxRetries: Int
) {
    constructor( processName:String,   exception:Throwable,   shouldRetry:Boolean,   retryCount: Int,   maxRetries: Int):
        this(processName, exception::class.java.name, exception.stackTraceToString(), exception.message, shouldRetry, retryCount, maxRetries)

    //Constructor with simple error and No Retries...
    constructor(processName: String, errorMessage:String): this(processName, null, null, errorMessage, false, 0, 0)
}