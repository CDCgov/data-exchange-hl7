package gov.cdc.dex.cloud.messaging

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import javax.inject.Inject

@MicronautTest
class TestStorageQueue {
    @Inject
    lateinit var messaging: CloudMessaging

    @Test
    fun receiveMessages() {
        println("testing receive messages")
        println(messaging.getQueueUrl())
        do {
            val msgs = messaging.receiveMessage()
            msgs.forEach {
                println(it.key())
            }
        } while (msgs.isNotEmpty())
    }
}