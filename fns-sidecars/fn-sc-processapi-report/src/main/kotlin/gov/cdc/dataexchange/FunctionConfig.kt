package gov.cdc.dataexchange

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusSenderClient

class FunctionConfig {
    val sbConnString = System.getenv("ServiceBusConnectionString")
    val sbQueue = System.getenv("ServiceBusQueue")
    val serviceBusSender : ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()

}