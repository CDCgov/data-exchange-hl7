package gov.cdc.dex.hl7

object ProcessInfo {
   val RECEIVER_PROCESS = "RECEIVER"
   val RECEIVER_VERSION = System.getenv("FN_VERSION")?.toString() ?: "Unknown"

}