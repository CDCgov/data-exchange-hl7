package gov.cdc.dex.logging

import com.google.gson.Gson
import java.util.*

data class LogModel(val service: String, val logLevel: String, val description: String, val messageUUID: String, val fileName: String, val timestamp: Long = Date().time) {

    fun toJson():String  {
        return Gson().toJson(this)
    }

    override fun toString():String {
        return this.toJson()
    }
}