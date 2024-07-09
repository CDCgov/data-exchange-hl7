package gov.cdc.dataexchange

import gov.cdc.dataexchange.model.Report
import gov.cdc.dex.util.JsonHelper.gson
import org.junit.jupiter.api.Test

class TestModels {
    @Test
    fun testEnums() {
        val report = Report(
            ingested_file_path = "test"
        )
        println(gson.toJson(report))
    }
}