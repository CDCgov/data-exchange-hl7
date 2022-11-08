package gov.cdc.dex.hl7.temp

const val SEPARATOR = "_"

  class EventCodeUtil () {
  companion object {
    fun getMMGName(eventCode: String): String {
      return when (eventCode) {
        // Event Code | MMG Name (Key in Redis) | MMG UUID
        "ANY" -> "TBRD"
        "11089" -> "LYME" // | MMG UUID
        "11080" -> "TBRD" // | MMG UUID
        "11088" -> "TBRD" // | MMG UUID
        "11090" -> "TBRD" // | MMG UUID
        "11091" -> "TBRD" // | MMG UUID
        "10250" -> "TBRD" // | MMG UUID
        else -> "Event code $eventCode not supported Yet!"
      } // .when

    } // .getMmgName
  }
} // .EventCodeUtil
