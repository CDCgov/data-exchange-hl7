package gov.cdc.dataexchange

import gov.cdc.dex.util.JsonHelper.gson
import org.junit.jupiter.api.Test

class TestReports {

    private fun testReport(testDataFilePath: String) {
        val testData = this::class.java.getResource(testDataFilePath).readText()
        val report = ReportTransformer().mapDataToBaseReport(testData)
        println(gson.toJson(report))
    }
    @Test
    fun testReceiverReport() {
        testReport("/recdeb-report.json")
    }

    @Test
    fun testRedactorReport() {
        testReport("/redactor-report.json")
    }

    @Test
    fun testStructReport() {
        testReport("/struct-report.json")
    }

    @Test
    fun testJsonReport() {
        testReport("/json-lake-report.json")
    }

    @Test
    fun testLakeSegReport() {
        testReport("/lake-seg-report.json")
    }
}