package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.EventHubStageMetadata
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class HL7JSONLakeStageMetadata(
    @Transient val jsonLakeStatus: String,
    val output: JsonObject?,
    @Transient val eventHubMD: EventHubMetadata,
    @Transient val config: List<String>
) : EventHubStageMetadata(
    stageName = PROCESS_NAME,
    stageVersion = PROCESS_VERSION,
    status = jsonLakeStatus, eventHubMetadata = eventHubMD, configs = config
) {

    companion object {
        const val PROCESS_NAME = "HL7-JSON-LAKE-TRANSFORMER"
        val PROCESS_VERSION = readVersionFromPomxml()

        private fun readVersionFromPomxml(): String {
            val xmlFile = File("pom.xml")
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
            val versionNode = doc.documentElement.getElementsByTagName("version").item(0)

            return versionNode.textContent
        }
    }
}
