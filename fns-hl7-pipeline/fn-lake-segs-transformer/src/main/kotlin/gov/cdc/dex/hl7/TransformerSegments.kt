package gov.cdc.dex.hl7

import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy

import gov.cdc.dex.hl7.model.Segment
import gov.cdc.dex.hl7.util.SegIdBuilder


class TransformerSegments()  {

    // global for class
    private var lakeFlatSegs: Array<Array<Pair<Int,String>>> = arrayOf()
    private var flatSegs: Array<Pair<Int,String>> = arrayOf()
    private var treeIndex: Int = 0
    private var segIds: MutableMap<Int, String> = mutableMapOf() // maps segment_number to segment_id


    fun hl7ToSegments(hl7Message: String, profile: String) : List<Segment> {

        val segmentPairs: Array<Array<Pair<Int,String>>> = hl7ToSegmentPairs(hl7Message, profile)

        // build segIds
        val segClient = SegIdBuilder()
        segmentPairs
            .map { seg -> seg.last() }
            .forEach { pair ->
                if (!segIds.containsKey(pair.first)) {
                    val segId = segClient.buildSegId(pair.second)
                    segIds[pair.first] = segId
                }
            }

        val segments = segmentPairs.map { segs ->
            val parentsPairs = segs.copyOfRange(0, segs.lastIndex)
            val parents = parentsPairs
                .filter{ pair -> pair.second != "root"} // remove root parent, not needed
                .map { pair ->
                    segIds[pair.first]!!
                }
                .reversed() // reverse order to put parent first, grandparent second, etc.

            Segment(segs.last().second, segs.last().first, segIds[segs.last().first]!!, parents)
        } // .segments
        
        return ArrayList(segments).apply { removeAt(0) } // remove empty non HL7 root
    } // .hl7ToSegments



    private fun hl7ToSegmentPairs(hl7Message: String, profile: String) : Array<Array<Pair<Int,String>>> {

        val msgTree = HL7HierarchyParser.parseMessageHierarchyFromJson(hl7Message, profile)
        travTreeToArr(msgTree)

        return lakeFlatSegs
    } // .hl7ToSegmentPairs


    private fun travTreeToArr(node: HL7Hierarchy) {

        // skip adding root string, only segments
        // if (node.segment() != "root") {
            flatSegs += Pair(treeIndex++, node.segment())
        // } // .if

        if ( node.children().isEmpty ) {
            // add this segments
            lakeFlatSegs += flatSegs 
            // clear out this child node to make room for a sibling node
            flatSegs = flatSegs.copyOfRange(0, flatSegs.lastIndex) 

        } else {
            // add this segment once
            lakeFlatSegs += flatSegs 

            // traverse children
            node.children().foreach { it: HL7Hierarchy -> 
                travTreeToArr(it)
            } // .foreach

            // if not root
            if (flatSegs.size > 0) {
                // done with children, clear out this last node to make room for sibling node
                flatSegs = flatSegs.copyOfRange(0, flatSegs.lastIndex) 
            } // .if

        } // .else 

    } // .travTreeToArr

} // .TransformerSql