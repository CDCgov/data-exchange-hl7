package test.gov.cdc.dex

import gov.cdc.dex.azure.health.DependencyChecker
import org.junit.jupiter.api.Test

class TestHealthCheck {

    @Test
    fun testServiceBusHealthCheck() {
        val sbConnString = System.getenv("ServiceBusConnectionString")
        val sbTopic = System.getenv("ServiceBusTopic")
        val sbChecker = DependencyChecker()
        val result =  sbChecker.checkServiceBusTopic(sbConnString, sbTopic)
        println(result.status)
        println(result.healthIssues)
    }
}