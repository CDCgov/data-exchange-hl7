package gov.cdc.dex.cloud.messaging

import gov.cdc.dex.cloud.ProviderMeta

interface CloudMessaging : ProviderMeta {
    fun listQueues(vararg prefixes: String): List<String>
    fun getQueueUrl(queueName: String): String
    fun getQueueUrl(): String
    fun receiveMessage(): List<CloudMessage>
    fun receiveMessage(queueName: String): List<CloudMessage>
    fun deleteMessage(message: CloudMessage): String
    fun timeoutMessage(message: CloudMessage, timeout: Int): String

    fun healthCheck(): String
}