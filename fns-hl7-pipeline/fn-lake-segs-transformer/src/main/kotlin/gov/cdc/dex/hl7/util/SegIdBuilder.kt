package gov.cdc.dex.hl7.util

class SegIdBuilder {
    private var segIdMap: MutableMap<String, Int> = mutableMapOf()

    fun buildSegId(seg: String): String {
        return if (seg != "root") {
            val segType = seg.substring(0, 3)
            val segIndex: Int = if (segIdMap.containsKey(segType)) {
                segIdMap[segType]?.plus(1)!!
            } else 1
            segIdMap[segType] = segIndex
            "$segType[$segIndex]"
        } else "root[0]"
    }

}