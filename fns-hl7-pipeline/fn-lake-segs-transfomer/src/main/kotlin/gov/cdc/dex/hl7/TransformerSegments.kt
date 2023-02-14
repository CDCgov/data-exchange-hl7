package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory

import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy 


class TransformerSegments()  {

    companion object {
        val logger = LoggerFactory.getLogger(TransformerSegments::class.java.simpleName)

    } // .companion object

    var lakeFlatSegs: Array<Array<String>> = arrayOf()
    var flatSegs: Array<String> = arrayOf()


    fun hl7ToSegments(hl7Message: String, profile: String) : Array<Array<String>> {

        val msgTree = HL7HierarchyParser.parseMessageHierarchyFromJson(hl7Message, profile)
        travTreeToArr(msgTree)

        return lakeFlatSegs
    } // .hl7ToSegments

    private fun travTreeToArr(node: HL7Hierarchy) {

        // skip adding root string, only segments
        if (node.segment() != "root") {
            flatSegs += node.segment()
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

