package gov.cdc.hl7.bumblebee

data class Profile(val segmentDefinition: Map<String, SegmentConfig>?,
                   val segmentFields: Map<String, Array<HL7SegmentField>>) {
    //Shortcut method to avoid profile.segments.segments call...
    fun getSegmentField(segmentName: String): Array<HL7SegmentField>? {
        //Need to go down to children to find segments...
        return this.segmentFields[segmentName]
    }
}

data class SegmentConfig(val cardinality: String,
                         val children: Map<String, SegmentConfig>)


data class HL7SegmentField(val fieldNumber: Int,
                           val name: String,
                           val dataType: String,
                           val maxLength: Int?,
                           val usage: String?,
                           val cardinality: String,
                           val default: String?,
                           val conformance: String?,
                           val notes: String?)
