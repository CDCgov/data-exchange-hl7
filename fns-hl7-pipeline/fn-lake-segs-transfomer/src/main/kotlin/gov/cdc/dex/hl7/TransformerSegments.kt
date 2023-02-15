package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory

import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy 

import gov.cdc.dex.hl7.model.Segment 


class TransformerSegments()  {

    companion object {
        val logger = LoggerFactory.getLogger(TransformerSegments::class.java.simpleName)

    } // .companion object

    // global for class
    var lakeFlatSegs: Array<Array<Pair<Int,String>>> = arrayOf()
    var flatSegs: Array<Pair<Int,String>> = arrayOf()
    var treeIndex: Int = 1 


    fun hl7ToSegments(hl7Message: String, profile: String) : List<Segment>{

        val segmentPairs: Array<Array<Pair<Int,String>>> = hl7ToSegmentPairs(hl7Message, profile)

        // get just one MSH out for first entry in flat model
        val mshSeg = segmentPairs.first().first() 
        val mshObj = listOf( Segment(mshSeg.second, mshSeg.first, null) )

        // pairs to segment 
        val flat = segmentPairs.map { segs -> 

            val parentsPairs = segs.copyOfRange(0, segs.lastIndex) 
            
            val parents = parentsPairs.map{ seg -> seg.second }

            Segment(segs.last().second, segs.last().first, parents)
        } // .flat 

        return mshObj.plus(flat)
    } // .hl7ToSegments



    private fun hl7ToSegmentPairs(hl7Message: String, profile: String) : Array<Array<Pair<Int,String>>> {

        val msgTree = HL7HierarchyParser.parseMessageHierarchyFromJson(hl7Message, profile)
        travTreeToArr(msgTree)

        return lakeFlatSegs
    } // .hl7ToSegmentPairs


    private fun travTreeToArr(node: HL7Hierarchy) {

        // skip adding root string, only segments
        if (node.segment() != "root") {
            flatSegs += Pair(treeIndex++, node.segment())
        } // .if

        if ( node.children().isEmpty ) {
            // add this segments
            lakeFlatSegs += flatSegs 
            // clear out this child node to make room for a sibling node
            flatSegs = flatSegs.copyOfRange(0, flatSegs.lastIndex) 

        } else {
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

