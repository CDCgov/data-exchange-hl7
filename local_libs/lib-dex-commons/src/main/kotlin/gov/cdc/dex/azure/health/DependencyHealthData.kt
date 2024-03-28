package gov.cdc.dex.azure.health

import com.google.gson.annotations.SerializedName

data class DependencyHealthData(
    val service: String,
    var status: String = "UP",
    @SerializedName("health_issues") var healthIssues: String = ""
)
