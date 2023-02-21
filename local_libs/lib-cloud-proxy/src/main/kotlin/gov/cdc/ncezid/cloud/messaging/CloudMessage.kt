package gov.cdc.ncezid.cloud.messaging

abstract class CloudMessage(
    val id: String,
    val recipientHandle: String,
    val body: String,
    val queue: String
) {
    abstract fun key(): String

    override fun toString(): String {
        return """CloudMessage(
            |  id='$id', 
            |  recipientHandle='$recipientHandle', 
            |  body='$body', 
            |  key='${key()}',
            |  queue='$queue'
            |)""".trimMargin()
    }
}
