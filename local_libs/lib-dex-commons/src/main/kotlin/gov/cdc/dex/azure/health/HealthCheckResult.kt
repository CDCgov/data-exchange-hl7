package gov.cdc.dex.azure.health

import com.google.gson.annotations.SerializedName

data class HealthCheckResult(
    var status: String = "UP",
    @SerializedName("total_checks_duration")
    var totalChecksDuration : String? = null,
    @SerializedName("dependency_health_checks")
    var dependencyHealthChecks : MutableList<DependencyHealthData> = mutableListOf()
)
data class DependencyHealthData(
    val service: String,
    var status: String = "UP",
    @SerializedName("health_issues") var healthIssues: String = ""
)