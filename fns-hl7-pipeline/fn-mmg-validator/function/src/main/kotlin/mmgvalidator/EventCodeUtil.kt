package com.example

const val SEPARATOR = "_"

class EventCodeUtil () {

  fun getMmgName(eventCode: String): String {

    return when( eventCode ) {
      // "11089" -> "LYME"
      "11080" -> "TBRD" 
      "11088" -> "TBRD"
      "11090" -> "TBRD"
      "11091" -> "TBRD"
      "10250" -> "TBRD"
      else -> HL7Error.EVENT_CODE_NOT_SUPPORTED_ERROR.message
    } // .when

  } // .getMmgName

} // .EventCodeUtil