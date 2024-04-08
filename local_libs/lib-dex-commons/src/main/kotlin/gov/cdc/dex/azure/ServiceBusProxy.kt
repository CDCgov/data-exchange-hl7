package gov.cdc.dex.azure

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class ServiceBusProxy(val sbConnString: String, val sbQueue: String) {
    private val serviceBusSender: ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()

    fun disconnect() {
        serviceBusSender.close()
    }
}