package gov.cdc.dataexchange

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class FunctionConfig {
    val sbConnString = System.getenv("ServiceBusConnectionString")
    val sbQueue = System.getenv("ServiceBusQueue")
    val maxMessageSize = try {
        System.getenv("MAX_SERVICE_BUS_MESSAGE_SIZE").toInt()
    } catch (e : Exception) {
        262144
    }

    val serviceBusSender : ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()

}