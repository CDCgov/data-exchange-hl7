package cdc.gov

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.lang.NumberFormatException
import javax.xml.parsers.DocumentBuilderFactory
import cdc.gov.StringUtils.Companion.normalize


class IgamtToBumblebeeTransformer () {
    private val gson: Gson = GsonBuilder().create()
    fun transformProfile(igamtProfilePath: String, outputPath: String, outputProfileName: String? = null) {
   //     try {
            val doc = loadDocumentFromFile(igamtProfilePath)
            val outputProfile = mutableMapOf<String, Any>()
            val profileName = if(outputProfileName.isNullOrEmpty()) {
                doc.documentElement.getElementsByTagName("MetaData").item(0)
                    .attributes.getNamedItem("Name").textContent.normalize()
            } else {
                outputProfileName
            }

            outputProfile["segmentDefinition"] = getSegmentDefinitionSection(doc)
            outputProfile["segmentFields"] = getFieldData(doc, "Segments", "Name")
            val dataTypesProfile = mutableMapOf<String, Any>()
            dataTypesProfile["segmentFields"] = getFieldData(doc, "Datatypes", "Label")
            saveFile("$outputPath/profile-${profileName}.json", gson.toJsonTree(outputProfile))
            saveFile("$outputPath/fields-${profileName}.json", gson.toJsonTree(dataTypesProfile))
            println("Saved files to $outputPath")
//        } catch (e: Exception) {
//            println("Error in transformer: ${e.message}")
//        }
    }

    private fun saveFile(fileName: String, contents: JsonElement) {
         File(fileName).writeText(contents.toString())

    }
    private fun getFieldData(doc: Document, sectionName: String, nameParameter: String): MutableMap<String, Any> {
        val segments = doc.documentElement.getElementsByTagName(sectionName).item(0)
        // each segment in Segments lists the Fields, which is the data we need.
        val segmentsMap = mutableMapOf<String,Any>()
        if (segments.childNodes.length > 2) {
            for (i in 3 until segments.childNodes.length step(2)) {
                val segment = segments.childNodes.item(i)
                val segmentName = segment.attributes.getNamedItem(nameParameter).textContent
                val fieldsArray = getFieldsForSegment(segment)
                if (fieldsArray.isNotEmpty())
                    segmentsMap[segmentName] = fieldsArray
            }
        }
        return segmentsMap
    }

    private fun getFieldsForSegment(segment: Node) : List<HL7SegmentField> {
        val fieldList = mutableListOf<HL7SegmentField>()
        var fieldNumber = 0
        if (segment.childNodes.length > 2) {
            for (i in 1 until segment.childNodes.length step(2)) {
                val field = segment.childNodes.item(i)
                if (field.hasAttributes()) {
                    fieldNumber++
                    val name = field.attributes.getNamedItem("Name").textContent
                    val datatype = field.attributes.getNamedItem("Datatype").textContent
                    val maxLength = try {
                        field.attributes.getNamedItem("MaxLength").textContent.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }

                    val usage = field.attributes.getNamedItem("Usage").textContent
                    val min = try {
                        field.attributes.getNamedItem("Min").textContent
                    } catch (e: Exception) {
                        "1"
                    }
                    val max = try {
                        field.attributes.getNamedItem("Max").textContent
                    } catch (e: Exception) {
                        "1"
                    }
                    val hl7Field = HL7SegmentField(
                        fieldNumber = fieldNumber,
                        name = name,
                        dataType = datatype,
                        maxLength = maxLength,
                        usage = usage,
                        cardinality = "[$min..$max]",
                        conformance = "",
                        notes = ""
                    )
                    fieldList.add(hl7Field)
                }
            }
        }
        return fieldList
    }

    private fun getSegmentDefinitionSection(doc: Document): MutableMap<String, Any> {
        val message = doc.documentElement.getElementsByTagName("Message").item(0)
        val nodeMap = mutableMapOf<String, Any>()
        if (message.hasChildNodes()) {
            for (i in 3 until message.childNodes.length step(2)) {
                // start with 3 because even numbered children are #text nodes,
                // which are empty,
                // and child 1 is MSH which is already handled
                val child = message.childNodes.item(i)
                if (child.nodeName == "Segment") {
                    processSegment(child, nodeMap)
                } else if (child.nodeName == "Group") {
                    if (child.childNodes.item(1).nodeName == "Group") {
                        //this is a group of groups
                        for (j in 1 until child.childNodes.length step(2))
                            processGroup(child.childNodes.item(j), nodeMap)
                    } else {
                        processGroup(child, nodeMap)
                    }
                }
            }
        }
        val messageMap = mutableMapOf<String, Any>()
        messageMap["MSH"] = mutableMapOf<String, Any>(Pair("cardinality", "[1..1]"), Pair("children", nodeMap))
        return messageMap
    }

    private fun processSegment(segmentNode: Node, nodeMap: MutableMap<String, Any>, children: MutableMap<String, Any>? = null) {
        val usage = segmentNode.attributes.getNamedItem("Usage").textContent
        val segName = segmentNode.attributes.getNamedItem("Ref").textContent.substring(0, 3)
        if (usage != "X") {
            val attribMap = mutableMapOf<String, Any?>()
            val min = segmentNode.attributes.getNamedItem("Min").textContent
            val max = segmentNode.attributes.getNamedItem("Max").textContent
            attribMap["cardinality"] = "[$min..$max]"
            attribMap["children"] = children
            nodeMap[segName] = attribMap
        }
    }

    private fun processGroup(groupNode: Node, nodeMap:MutableMap<String, Any>, startWith: Int = 3) {
        var child = groupNode
        while (child.nodeName != "Segment") {  child = child.childNodes.item(1) }
        // now we are at the top of the group -- this one has "children" IF its min cardinality > 0

        // set cardinality of main node to that of the group
        val minAttrib = child.attributes.getNamedItem("Min")
        if (minAttrib.textContent.toInt() > 0) {
            val mainNode = child.cloneNode(false)
            val maxAttrib = mainNode.attributes.getNamedItem("Max")
            val parentGroup = child.parentNode
            minAttrib.textContent = parentGroup.attributes.getNamedItem("Min").textContent
            maxAttrib.textContent = parentGroup.attributes.getNamedItem("Max").textContent

            val newNodeMap = mutableMapOf<String, Any>()
            for (j in startWith until parentGroup.childNodes.length step (2)) {
                if (parentGroup.childNodes.item(j).nodeName == "Segment")
                    processSegment(parentGroup.childNodes.item(j), newNodeMap)
                else if (parentGroup.childNodes.item(j).nodeName == "Group") {
                    processGroup(parentGroup.childNodes.item(j), newNodeMap)
                }
            }
            processSegment(mainNode, nodeMap, newNodeMap)
        } else {
            processSegment(child, nodeMap)
            processGroup(child.nextSibling.nextSibling, nodeMap, startWith + 2)
        }
    }


    private fun loadDocumentFromFile(filePath: String) : Document {
        val inputFile = File(filePath).readText().replace("><", ">\n<")
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(InputSource(StringReader(inputFile)))
        doc.documentElement.normalize()
        return doc
    }

}