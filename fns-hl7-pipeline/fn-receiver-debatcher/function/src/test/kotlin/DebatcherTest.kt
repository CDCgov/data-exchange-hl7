import com.azure.storage.blob.BlobServiceClientBuilder
import com.example.UTF_BOM
import org.junit.jupiter.api.Test

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DebatcherTest {


    @Test
    fun testRemoveBom() {
        //val testFile = this::class.java.getResource("/Genv2_2-0-1_TC08.txt")
        val reader = File("src/test/resources/Genv2_2-0-1_TC08.txt").bufferedReader()
        BufferedReader( reader ).use { br ->
            br.forEachLine { line ->
                //while ( br.readLine().also { line = it } != null) {
                //println(line)
                var lineClean = line.trim()
                while (lineClean.startsWith(UTF_BOM)) {
                    //if (lineClean.startsWith(UTF_BOM)) {
                    println("Found BOM...")
                    lineClean = lineClean.substring(1)
                    println(line.length)
                    println(lineClean.length)
                }
            }
        }
    }

    @Test
    fun testRemoveBomFromBlob() {
        val blobContainerClient = BlobServiceClientBuilder()
            .connectionString("DefaultEndpointsProtocol=https;AccountName=tfedemessagestoragedev;AccountKey=aQdC1eVh+c0xiYFjXKAbBO2Dj+cBTu4R/yEFyFShW4o90D9fpUpaC4TxD3b+Z11Y/VxNDXMsBGGw+AStlIuNyQ==;EndpointSuffix=core.windows.net")
            .buildClient()
            .getBlobContainerClient("hl7ingress")

        val blobClient = blobContainerClient.getBlobClient("Genv2_2-0-1_TC08.txt")
        val reader = InputStreamReader( blobClient.openInputStream(), Charsets.UTF_8 )
        BufferedReader( reader ).use { br ->
            br.forEachLine { line ->
                var lineClean = line.trim()
                while (lineClean.startsWith(UTF_BOM)) {
                    //if (lineClean.startsWith(UTF_BOM)) {
                    println("Found BOM...")
                    lineClean = lineClean.substring(1)
                    println(line.length)
                    println(lineClean.length)
                }
            }
        }
    }
}