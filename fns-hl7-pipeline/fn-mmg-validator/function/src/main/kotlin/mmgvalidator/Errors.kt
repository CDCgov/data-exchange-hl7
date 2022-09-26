package com.example

interface Error {
    fun message(): String
} // .Error 


enum class HL7Error(val message: String) : Error {

    PROFILE_ID_ERROR("Error extracting profile identifier from hl7 message"),
    EVENT_CODE_ERROR("Error extracting even code from hl7 message"),
    EVENT_CODE_NOT_SUPPORTED_ERROR("MMG for this message event code is not available");

    override fun message(): String {
        return this.message
    } // .message 

} // .HL7Error