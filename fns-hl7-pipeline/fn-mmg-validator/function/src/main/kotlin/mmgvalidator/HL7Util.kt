package com.example

import open.HL7PET.tools.HL7StaticParser

class HL7Util () {

  fun getProfileIdentifier( msg: String ): String {

    val msgValue = HL7StaticParser.getFirstValue(msg, "MSH-21[2].1") //  "OBR[@3.1='68991-9']-31.1"
    if (msgValue.isDefined)
      return msgValue.get()
    else
      return HL7Error.PROFILE_ID_ERROR.message

  } // .getProfileIdentifier

  fun getEventCode( msg: String ): String {

    val msgValue = HL7StaticParser.getFirstValue(msg, "OBR[@4.1='68991-9']-31.1") //  "OBR[@3.1='68991-9']-31.1"
    if (msgValue.isDefined)
      return msgValue.get()
    else
      return HL7Error.EVENT_CODE_ERROR.message


  } // .getEventCode

} // .HL7Util