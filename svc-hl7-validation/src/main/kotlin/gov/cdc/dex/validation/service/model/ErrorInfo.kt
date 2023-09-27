package gov.cdc.dex.validation.service.model
import com.google.gson.annotations.SerializedName

data class ErrorInfo (
    val line: Int = 0,
    val column: Int = 0,
    val path : String? = "",
    val description : String,
    val category : String = "Runtime Error",
    val classification : String = "Error",
    val stackTrace:  List<String>? = null,
    val metaData : Map<String, String>? = null
)
