package gov.cdc.dex.azure

import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusClientBuilder.ServiceBusReceiverClientBuilder
import com.azure.messaging.servicebus.ServiceBusReceiverClient
import com.azure.messaging.servicebus.ServiceBusSenderClient

class ServiceBusProxy(val sbConnString: String, val sbQueue: String) {
    val serviceBusSender: ServiceBusSenderClient = ServiceBusClientBuilder()
        .connectionString(sbConnString)
        .sender()
        .queueName(sbQueue)
        .buildClient()
//    val serviceBusReceiverClientBuilder = ServiceBusReceiverClientBuilder()
//    val sbReader=
//        QueueClient(ConnectionStringBuilder(connectionString,queueName),ReceiveMode.PEEKLOCK).conn
}