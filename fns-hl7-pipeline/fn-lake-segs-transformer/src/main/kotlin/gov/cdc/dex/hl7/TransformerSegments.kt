package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory

import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy 

import gov.cdc.dex.hl7.model.Segment
import kotlin.collections.isNotEmpty 


class TransformerSegments()  {

    companion object {
        val logger = LoggerFactory.getLogger(TransformerSegments::class.java.simpleName)

    } // .companion object

    // global for class
    var lakeFlatSegs: Array<Array<Pair<Int,String>>> = arrayOf()
    var flatSegs: Array<Pair<Int,String>> = arrayOf()
    var treeIndex: Int = 0


    fun hl7ToSegments(hl7Message: String, profile: String) : List<Segment> {

        val segmentPairs: Array<Array<Pair<Int,String>>> = hl7ToSegmentPairs(hl7Message, profile)

        val segments = segmentPairs.map { segs -> 

            val parentsPairs = segs.copyOfRange(0, segs.lastIndex)
            
            val parents = parentsPairs.map{ seg -> seg.second }.filter{ s -> s != "root"} // remove root parent, not needed

            Segment(segs.last().second, segs.last().first, parents)
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

