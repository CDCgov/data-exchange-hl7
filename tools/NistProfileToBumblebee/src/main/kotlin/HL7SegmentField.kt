package cdc.gov

data class HL7SegmentField(val fieldNumber: Int,
                           val name: String,
                           val dataType: String,
                           val maxLength: Int?,
                           val usage: String?,
                           val cardinality: String,
                           val conformance: String?,
                           val notes: String?)
