package gov.cdc.dataexchange

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class FunctionConfig {
    val sbConnString = System.getenv("ServiceBusConnectionString")
    val sbTopic = System.getenv("ServiceBusTopic")
    val maxMessageSize = try {
        System.getenv("MAX_SERVICE_BUS_MESSAGE_SIZE").toInt()
    } catch (e : Exception) {
        262144
    }

    val serviceBusSender : ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .topicName(sbTopic)
        .buildClient()

}