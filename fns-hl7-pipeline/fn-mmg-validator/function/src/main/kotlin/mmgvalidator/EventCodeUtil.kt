package com.example

const val SEPARATOR = "_"

class EventCodeUtil () {

  fun getMmgName(eventCode: String): String {

    return when( eventCode ) {

      // Event Code | MMG Name (Key in Redis) | MMG UUID
      "11089" -> "LYME" // | MMG UUID
      "11080" -> "TBRD" // | MMG UUID
      "11088" -> "TBRD" // | MMG UUID
      "11090" -> "TBRD" // | MMG UUID
      "11091" -> "TBRD" // | MMG UUID
      "10250" -> "TBRD" // | MMG UUID
      else -> HL7Error.EVENT_CODE_NOT_SUPPORTED_ERROR.message
    } // .when

  } // .getMmgName

} // .EventCodeUtil