package gov.cdc.dex.azure

import com.azure.core.credential.TokenCredential
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class ServiceBusProxy(serviceBusNamespace: String, sbQueue: String, tokenCredential: TokenCredential) {
    private val serviceBusSender: ServiceBusSenderClient = ServiceBusClientBuilder()
        .credential(serviceBusNamespace, tokenCredential)
        .sender()
        .queueName(sbQueue)
        .buildClient()

    fun disconnect() {
        serviceBusSender.close()
    }
}