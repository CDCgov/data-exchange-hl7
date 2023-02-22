package gov.cdc.ncezid.cloud.storage

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.util.*

import javax.inject.Inject

@MicronautTest
internal class CloudStorageProxyTest {

    @Inject
    lateinit var cloudStorage: CloudStorage

    @Test
    fun listFolders() {
//        val configs = s3Proxy.listFolders("cf-daart-hl7-dropbucket-dev")
//        val configs = cloudStorage.listFolders("cf-daart-nist-profiles-dev")
        val configs = cloudStorage.list("foodnet", 10)
        configs.forEach { println("Folder: $it")}
        if (configs.isEmpty()) {
            println("NO FOLDERS FOUND ON THIS BUCKET!")
        }
    }

    @Test
    fun testListFilesInFolder() {
        val configs = cloudStorage.list(5, "ingress/hl7/QA/FDD/FDD_MMG_V1.0/")
        configs.forEach { println("file: $it")}
    }

    @Test
    fun testGetFile() {
        val file = cloudStorage.getFile("config/config.json")
        println("${file.fileName} =>  ${file.content}")
    }
    @Test
    fun testUploadFileNoMetadata() {
        val file = cloudStorage.saveFile("foodnet", "testFile-${UUID.randomUUID()}.txt", "Hi There", null, "text/plain" )
    }

    @Test
    fun testUploadFileWithMetadata() {
        val metadata = mapOf("test" to "meta1")
        val file = cloudStorage.saveFile("foodnet", "/ingress/hl7/test/FDD/testFile-${UUID.randomUUID()}.txt", "Hi There", metadata, "text/plain" )
    }
}