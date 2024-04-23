package gov.cdc.dex
import com.azure.identity.DefaultAzureCredential
import com.azure.identity.DefaultAzureCredentialBuilder
import gov.cdc.dex.azure.AzureBlobProxy
import org.junit.jupiter.api.Test


class TestAzureBlobProxy {
    @Test
    fun testBlobProxyCredential() {
        val url = "https://ocioedemessagesadev.core.windows.net"
        val container = "hl7ingress"
        val credential = DefaultAzureCredentialBuilder().build()

        val blobProxy = AzureBlobProxy(url, container, credential)
    }

}