import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
// import kotlin.test.assertFailsWith
// import kotlin.test.assertFails

import org.slf4j.LoggerFactory
import gov.cdc.dex.hl7.TransformerSegments

// No longer used
// import com.google.gson.Gson
// import com.google.gson.GsonBuilder
// import com.google.gson.reflect.TypeToken

import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy 

class LakeSegsTransTest {

    companion object {

        val logger = LoggerFactory.getLogger(LakeSegsTransTest::class.java.simpleName)
    } // .companion 


    @Test
    fun testLoggerInfo() {

        logger.info("testLoggerInfo...")
    } // .testLoggerInfo

    // No longer used
    // @Test
    // fun testLoadBasicProfileWithGson() {

    //     val profileFilePath = "/BasicProfile.json"
    //     val profileMapJson = this::class.java.getResource(profileFilePath).readText()
    //     val profileMapType = object : TypeToken< Profile >() {}.type

    //     val basicProfile: Profile = Gson().fromJson(profileMapJson, profileMapType)
    
    //     logger.info("basicProfile: --> $basicProfile")
    // } // .testLoadBasicProfileWithGson


    @Test
    fun testReadLocalResources() {

        logger.info("testReadLocalResources...")

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()

        assertEquals(profile.substring(0, 100).length, 100)


        // read the message:
        val messagePath = "/Genv2_2-0-1_TC01.hl7"
        val message = this::class.java.getResource(messagePath).readText()

        assertEquals(message.substring(0, 100).length, 100)
    } // .testParseMessageHierarchyFromJson


    @Test
    fun testHierarchyParser() {  

        logger.info("testHierarchyParser...")
        
        fun printTree(node: HL7Hierarchy, ident: String) {   
            println("$ident --> ${node.segment()}")  
             node.children().foreach {   
                printTree(it, ident + " ")
            }
        } // .printTree


        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()

        // read the message:
        val messagePath = "/Genv2_2-0-1_TC01.hl7"
        val message = this::class.java.getResource(messagePath).readText()

        val msgTree = HL7HierarchyParser.parseMessageHierarchyFromJson(message, profile)
        printTree(msgTree, "")
    } // .testHierarchyParser



    @Test
    fun testTransformerSegments() {

        logger.info("testTransformerSegments...")

        
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val testMsg100 = testMsg.substring(0, 100)
        assertEquals(testMsg100.length, 100)

        val lakeSegsModel = TransformerSegments().hl7ToSegments(testMsg)

        logger.info(lakeSegsModel.toString().substring(0, 10))

    } // .testTransformerSegments


} // .MmgSLakeSegsTransTestqlTest



