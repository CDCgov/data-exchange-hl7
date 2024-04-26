package gov.cdc.dataexchange

import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class FunctionConfig {
    private val sbNamespace = System.getenv("ServiceBusNamespace")
    private val sbClientId = System.getenv("ServiceBusClientId")
    lateinit var serviceBusSender: ServiceBusSenderClient
    val sbQueue = System.getenv("ServiceBusQueue")
    val maxMessageSize = try {
        System.getenv("MAX_SERVICE_BUS_MESSAGE_SIZE").toInt()
    } catch (e : Exception) {
        262144
    }
    init {
        createServiceBusSender()
    }
    fun createServiceBusSender() {
        val tokenCredential = DefaultAzureCredentialBuilder()
            .managedIdentityClientId(sbClientId)
            .build()

        serviceBusSender = ServiceBusClientBuilder()
            .credential(sbNamespace, tokenCredential)
            .sender()
            .queueName(sbQueue)
            .buildClient()
    }
}