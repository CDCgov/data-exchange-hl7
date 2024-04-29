package gov.cdc.dex
import com.azure.identity.DefaultAzureCredentialBuilder
import gov.cdc.dex.azure.DedicatedEventHubSender
import org.junit.jupiter.api.Test


class TestDedicatedEHSender {
    @Test
    fun testDedicatedSenderCredential() {
        val ehNamespace = "ocio-ede-dev-eventhub-namespace.servicebus.windows.net"
        val ehHubName = "hl7-recdeb-reports"
        val clientId = System.getenv("EventHubClientId")

        val token = DefaultAzureCredentialBuilder()
            .managedIdentityClientId(clientId)
            .build()

        val sender = DedicatedEventHubSender(ehNamespace, ehHubName, token)
        val ids = sender.getPartitionIds()
        ids.forEach { println(it) }
        sender.disconnect()
    }
}