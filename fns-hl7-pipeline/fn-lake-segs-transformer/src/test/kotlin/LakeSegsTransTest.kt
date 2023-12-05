import com.google.gson.Gson
import com.google.gson.GsonBuilder
import gov.cdc.dex.hl7.TransformerSegments
import gov.cdc.dex.hl7.util.SegIdBuilder
import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals


class LakeSegsTransTest {

    companion object {

        val logger = LoggerFactory.getLogger(LakeSegsTransTest::class.java.simpleName)
        
        // private val gson = Gson()
        private val gsonWithNullsOn = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()
    } // .companion 


    @Test
    fun testLoggerInfo() {

        logger.info("testLoggerInfo...")
    } // .testLoggerInfo


    @Test
    fun testReadLocalResources() {

        logger.info("testReadLocalResources...")

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()

        assertEquals(100, profile.substring(0, 100).length)


        // read the message:
        val messagePath = "/Genv2_2-0-1_TC01.hl7"
        val message = this::class.java.getResource(messagePath).readText()

        assertEquals(100, message.substring(0, 100).length)
    } // .testReadLocalResources


    @Test
    fun testPrintHL7HierarchyTree() {

        var i = 0

        fun printTree(node: HL7Hierarchy, ident: String) {   
            
            println("$ident ($i) --> ${node.segment().substring(0,3)}")   
            i++
            node.children().foreach { 
                printTree(it, ident + " ")
            }
        } // .printTree 

        logger.info("testPrintHL7HierarchyTree...")
        
        val filePath = "/Hepatitis_V1_0_1_TM_TC02_HEP_B_ACUTE_MOD.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()


        val msgTree = HL7HierarchyParser.parseMessageHierarchyFromJson(testMsg, profile)
        printTree(msgTree, "")

    } // .testPrintHL7HierarchyTree



    @Test
    fun testTransformerSegments() {

        logger.info("testTransformerSegments...")
        
        val filePath = "/Hepatitis_V1_0_1_TM_TC02_HEP_B_ACUTE_MOD.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()


        val lakeSegsModel = TransformerSegments().hl7ToSegments(testMsg, profile)

        logger.info("lakeSegsModel: --> $lakeSegsModel")

        assertEquals(12, lakeSegsModel.size)
    } // .testTransformerSegments


    @Test
    fun testTransformerSegmentsToJson() {

        logger.info("testTransformerSegments...")
        
        val filePath = "/Hepatitis_V1_0_1_TM_TC02_HEP_B_ACUTE_MOD.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()


        val lakeSegsModel = TransformerSegments().hl7ToSegments(testMsg, profile)

        val lakeSegsModelJson = gsonWithNullsOn.toJson(lakeSegsModel)

        logger.info("lakeSegsModelJson: --> $lakeSegsModelJson")

        assertEquals(100, lakeSegsModelJson.substring(0, 100).length)
    } // .testTransformerSegmentsToJson

    @Test
    fun testTransformerSegmentsSegmentId() {

        logger.info("testTransformerSegmentsSegmentId...")

        val filePath = "/Hepatitis_V1_0_1_TM_TC02_HEP_B_ACUTE_MOD.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()

        val lakeSegsModel = TransformerSegments().hl7ToSegments(testMsg, profile)

        val gsonPretty = GsonBuilder().setPrettyPrinting().create()
        val prettyJson = gsonPretty.toJson(lakeSegsModel)
        println(prettyJson)

        val segClient = SegIdBuilder()
        lakeSegsModel.forEach { seg ->
            val expectedSegId = segClient.buildSegId(seg.segment)
            assertEquals(expectedSegId, seg.segmentId)
        }
    } // .testTransformerSegmentsSegmentId

} // .MmgSLakeSegsTransTestqlTest



