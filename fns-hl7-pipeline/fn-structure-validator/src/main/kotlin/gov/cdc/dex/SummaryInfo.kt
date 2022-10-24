package gov.cdc.dex

import com.google.gson.annotations.SerializedName


data class SummaryInfo(
    @SerializedName("current_status") val currentStatus: String,
    val problem: Problem
)

data class Problem(
    @SerializedName("process_name") val processName: String,
    @SerializedName("exception_class") val exceptionClass: String,
    @SerializedName("stacktrace") val stackTrace: String,
    @SerializedName("error_message") val errorMessage: String?,
    @SerializedName("should_retry") val shouldRetry: Boolean,
    @SerializedName("retry_count") val retryCount: Int,
    @SerializedName("max_retries") val maxRetries: Int
) {
    constructor( processName:String,   exception:Throwable,   shouldRetry:Boolean,   retryCount: Int,   maxRetries: Int):
        this(processName, exception::class.java.name, exception.stackTraceToString(), exception.message, shouldRetry, retryCount, maxRetries)
}