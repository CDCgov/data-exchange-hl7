package gov.cdc.hl7.bumblebee

import org.junit.jupiter.api.Test
import com.google.gson.JsonParser
import gov.cdc.hl7.HL7StaticParser

//import gov.cdc.ncezid.bumblebee.TemplateTransformer
class TestTransformation {


    val EPI_OBR = "68991-9"
    @Test
    fun testTransformation() {
        val bumblebee = TemplateTransformer.getTransformerWithResource("/labTemplate.json", "/BasicProfile.json")
        val message = this::class.java.getResource("/RawHL7DataDAART.txt").readText()

        //val obrs = HL7StaticParser.getValue(message, "OBR")

        val lines =message.split("\n")

        var oneLab = ""
        var foundOBR = false
        lines.forEach {
            if (it.startsWith("OBR|")) { //New Lab?
                //have we collected a lab yet?
                if (foundOBR) {
                    val newMessage = bumblebee.transformMessage(oneLab)
                    val json = JsonParser.parseString(newMessage)
                    println("=================")
                    println(json)
                    println("=================")
                }
                foundOBR = true
                oneLab = it + "\n"
            } else
                oneLab += it + "\n"
        }
        //process last OBR:
        val newMessage = bumblebee.transformMessage(oneLab)
        val json = JsonParser.parseString(newMessage)
        println("=================")
        println(json)
        println("=================")

    }
//        obrs.get().iterator().forEach {obrsInnerArray ->
//            obrsInnerArray.iterator().forEach {
//
//                val obr_4 = HL7StaticParser.getFirstValue(it, "OBR[1]-4.1")
//                println(obr_4)
//                if (EPI_OBR != obr_4.get() ) { //DO not convert EPI OBRS into Lab Orders
//                    val obxs = HL7StaticParser.getValue(message, "OBR[@4.1='${obr_4.get()}']->OBX")
//                    val spm = HL7StaticParser.getValue(message, "OBR[@4.1='${obr_4.get()}']->SPM")
//
//                    var oneLab = it + "\n"
//                    if (obxs.isDefined) {
//                        obxs.get().iterator().forEach { obxInnerArray ->
//                            obxInnerArray.iterator().forEach { obx ->
//                                oneLab += obx + "\n"
//                            }
//                        }
//                    }
//                    if (spm.isDefined) {
//                        oneLab += spm.get()
//                    }
//
//                    val newMessage = bumblebee.transformMessage(oneLab)
//
//                    val json = JsonParser.parseString(newMessage)
//                    println("=================")
//                    println(json)
//                    println("=================")
//                }

//            }
//        }
//        val newMessage = bumblebee.transformMessage(message)
//
//        val json = JsonParser.parseString(newMessage)
//        println(json)
//    }
}