package gov.cdc.dex

import org.junit.jupiter.api.Test
import com.google.gson.JsonParser
import gov.cdc.hl7.HL7ParseUtils
import gov.cdc.hl7.HL7StaticParser

//import gov.cdc.dex.TemplateTransformer
class TestTransformation {


    val EPI_OBR = "68991-9"
    @Test
    fun testTransformation() {
        val bumblebee = TemplateTransformer.getTransformerWithResource("/labTemplate.json", "/BasicProfile.json")
        val message = this::class.java.getResource("/RawHL7DataCELR.txt").readText()
        val hl7Parser : HL7ParseUtils = HL7ParseUtils.getParser(message, "BasicProfile.json")
        val obrs = hl7Parser.getValue("OBR").get().toList()
        val epiOBRs = hl7Parser.getValue("OBR[@4.1='$EPI_OBR||NOTF||PERSUBJ']").get().toList()
        val nonEpiOBRs = obrs.filter{ innerArray -> innerArray !in epiOBRs }
        println("obrs: ${obrs.size}, epiOBRs: ${epiOBRs.size}, nonEpiOBRS: ${nonEpiOBRs.size}")

//
//        val lines =message.split("\n")
//
//        var oneLab = ""
//        var foundOBR = false
//        lines.forEach {
//            if (it.startsWith("OBR|")) { //New Lab?
//                //have we collected a lab yet?
//                if (foundOBR) {
//                    val newMessage = bumblebee.transformMessage(oneLab)
//                    val json = JsonParser.parseString(newMessage)
//                    println("=================")
//                    println(json)
//                    println("=================")
//                }
//                foundOBR = true
//                oneLab = it + "\n"
//            } else
//                oneLab += it + "\n"
//        }
//        //process last OBR:
//        val newMessage = bumblebee.transformMessage(oneLab)
//        val json = JsonParser.parseString(newMessage)
//        println("=================")
//        println(json)
//        println("=================")

    //}
 /*       nonEpiOBRs.get().iterator().forEach {obrsInnerArray ->
            obrsInnerArray.iterator().forEach {

                val obr_4 = HL7StaticParser.getFirstValue(it, "OBR[1]-4.1")
                println(obr_4.get())
                if (obr_4.get() !in listOf(EPI_OBR, "NOTF", "PERSUBJ") ) { //DO not convert EPI OBRS into Lab Orders
                    val obxs = hl7Parser.getValue("OBR[@4.1='${obr_4.get()}']->OBX")
                    val spm = hl7Parser.getValue( "OBR[@4.1='${obr_4.get()}']->SPM")

                    var oneLab = it + "\n"
                    if (obxs.isDefined) {
                        obxs.get().iterator().forEach { obxInnerArray ->
                            obxInnerArray.iterator().forEach { obx ->
                                oneLab += obx + "\n"
                            }
                        }
                    }
                    if (spm.isDefined) {
                        oneLab += spm.get()
                    }

                    val newMessage = bumblebee.transformMessage(oneLab)

                    val json = JsonParser.parseString(newMessage)
                    println("=================")
                    println(json)
                    println("=================")
                } else {
                    println("EPI OBR is in the list")
                }

            }
        }
*/
    }
}