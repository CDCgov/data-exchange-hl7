import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
// import kotlin.test.assertFailsWith
// import kotlin.test.assertFails

import org.slf4j.LoggerFactory
import gov.cdc.dex.hl7.TransformerSegments

// No longer used
// import com.google.gson.Gson
import com.google.gson.GsonBuilder
// import com.google.gson.reflect.TypeToken


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

        assertEquals(profile.substring(0, 100).length, 100)


        // read the message:
        val messagePath = "/Genv2_2-0-1_TC01.hl7"
        val message = this::class.java.getResource(messagePath).readText()

        assertEquals(message.substring(0, 100).length, 100)
    } // .testReadLocalResources



    @Test
    fun testTransformerSegments() {

        logger.info("testTransformerSegments...")
        
        val filePath = "/Hepatitis_V1_0_1_TM_TC02_HEP_B_ACUTE_MOD.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()


        val lakeSegsModel = TransformerSegments().hl7ToSegments(testMsg, profile)

        // print the lake
        lakeSegsModel.forEach { segments ->

            segments.forEachIndexed { i, sg -> 
                if (i != segments.lastIndex) print("$sg, ")
                else print(sg)
            } // .forEachIndexed 

            println()

        }// .forEach

        assertEquals(lakeSegsModel.size, 8)

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

        assertEquals(lakeSegsModelJson.substring(0, 100).length, 100)
    } // .testTransformerSegmentsToJson


} // .MmgSLakeSegsTransTestqlTest



