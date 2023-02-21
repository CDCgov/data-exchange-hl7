package gov.cdc.ncezid.cloud.messaging.azure

import com.jayway.jsonpath.JsonPath
import gov.cdc.ncezid.cloud.messaging.CloudMessage

class ASQMessage (
    id: String,
    recipientHandle: String,
    body: String,
    queue: String
) : CloudMessage(id, recipientHandle, body, queue) {

    private val ASQ_KEY_PATH:String = "data.url"

    override fun key(): String {
        return JsonPath.parse(this.body).read(ASQ_KEY_PATH, String::class.java)
    }
}