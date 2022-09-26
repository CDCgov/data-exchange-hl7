package com.example

import open.HL7PET.tools.HL7StaticParser

class HL7Util () {

  fun getProfileID( msg: String ): String {

    // val msgValue = HL7StaticParser.getFirstValue(msg, "MSX-21[2].1")
    // if (msgValue.isDefined)
    //   return msgValue.get()
    // else
      return "No value found"


  } // .getProfileID

} // .HL7Util