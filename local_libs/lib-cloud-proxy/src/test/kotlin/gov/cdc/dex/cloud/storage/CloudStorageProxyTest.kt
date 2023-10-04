package gov.cdc.dex.cloud.storage

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

import javax.inject.Inject

@MicronautTest
@Tag("UnitTest")
internal class CloudStorageProxyTest {

    @Inject
    lateinit var cloudStorage: CloudStorage

    @Test
    fun listFolders() {
        val configs = cloudStorage.listFolders("hl7ingress")
        configs.forEach { println("Folder: $it")}
        assertTrue(configs.isNotEmpty(), "Asserting folders exist in container hl7ingress")
    }

    @Test
    fun testListFilesInFolder() {
        val configs = cloudStorage.list(10, "/")
        configs.forEach { println("file: $it")}
    }

//    @Test
//    fun testGetFile() {
//        val file = cloudStorage.getFile("unitTests/Lyme_HappyPath.doNotDelete.txt")
//        assertNotNull(file, "Asserting file is not null")
//    }
    @Test
    fun testUploadFileNoMetadata() {
        val file = cloudStorage.saveFile("hl7ingress", "testFile-${UUID.randomUUID()}.txt", "Hi There", null, "text/plain" )
        assertNotNull(file, "Asserting file uploaded")
    }

    @Test
    fun testUploadFileWithMetadata() {
        val metadata = mapOf("test" to "meta1")
        val file = cloudStorage.saveFile("hl7ingress", "/test/testFile-${UUID.randomUUID()}.txt", "Hi There", metadata, "text/plain" )
        assertNotNull(file, "Asserting file with metadata uploaded")
    }
}