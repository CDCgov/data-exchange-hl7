package gov.cdc.ncezid.cloud.messaging.aws

import com.jayway.jsonpath.JsonPath
import gov.cdc.ncezid.cloud.messaging.CloudMessage

private const val SQS_KEY_PATH = "$['Records'][0]['s3']['object']['key']"

class SQSMessage (
    id: String,
    recipientHandle: String,
    body: String,
    queue: String
) : CloudMessage(id, recipientHandle, body, queue) {

    override fun key(): String = JsonPath.parse(this.body).read(SQS_KEY_PATH, String::class.java)
}
