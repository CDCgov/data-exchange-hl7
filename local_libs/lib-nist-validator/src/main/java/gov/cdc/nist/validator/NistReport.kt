package gov.cdc.nist.validator

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import gov.nist.validation.report.Entry
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

//JsonProperty is used for jackson
//SerialiedName is used for Gson.
@JsonInclude(JsonInclude.Include.NON_NULL)
class NistReport {
    val entries: Entries = Entries()
    @JsonProperty("error-count")
    @SerializedName("error-count")
    var errorCounts: SummaryCount? = null
    @JsonProperty  ("warning-count")
    @SerializedName("warning-count")
    var warningcounts: SummaryCount?  = null
    var status:String? = null

    fun transferErrorCounts(map: Map<*, AtomicInteger>?) {
        this.errorCounts = transferCounts(map)
    }
    fun transferWarningCounts(map: Map<*,AtomicInteger>?) {
        this.warningcounts = transferCounts(map)
    }

    private fun transferCounts(map: Map<*, AtomicInteger>?):SummaryCount {
        return SummaryCount(
       map?.get("structure")?.get() ?:0,
            map?.get("value-set")?.get() ?:0,
       map?.get("content")?.get() ?:0
        )
    }


}

data class SummaryCount(
    val structure: Int  ,
    @JsonProperty  ("value-set")
    @SerializedName("value-set")
    val valueset: Int,
    val content: Int
)

class Entries {
    var structure = listOf<Entry>()
    var content   = listOf<Entry>()
    @JsonProperty  ("value-set")
    @SerializedName("value-set")
    var valueset  = listOf<Entry>()
}
//
//@JsonIgnoreProperties(ignoreUnknown = true)
//class Entry {
//     var line = 0
//     var column = 0
//     var path: String? = null
//     var description: String? = null
//     var category: String? = null
//     var classification: String? = null
//
//}