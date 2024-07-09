package gov.cdc.dataexchange

import org.junit.jupiter.api.Test

class TestReports {
    @Test
    fun testReceiverReport() {
        val testData = this::class.java.getResource("/recdeb-report.json").readText()
        val report = ReportTransformer().mapDataToBaseReport(testData)
        println(report)
    }
}