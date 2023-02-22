package gov.cdc.ncezid.cloud.storage

import gov.cdc.ncezid.cloud.AWSConfig
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

import kotlin.system.measureTimeMillis

@MicronautTest
class TestStreamRead(val awsConfig: AWSConfig) {

    fun String.countOccurrences(ch: Char = '\n'): Int {
        return this.filter { it == ch }.count()
    }

    val MILLI_IN_SEC = 1000
    val MILLI_IN_MIN = MILLI_IN_SEC * 60
    val MILLI_IN_HOURS = MILLI_IN_MIN * 60
    val MILLI_IN_DAYS = MILLI_IN_HOURS * 24

    fun Long.format() : String {
        val sb = StringBuilder()
        var totalTime = this
        if (totalTime > MILLI_IN_DAYS) {
            val days:Long  = (totalTime / MILLI_IN_DAYS)
            sb.append( " $days D ")
            totalTime -= (days*MILLI_IN_DAYS)
        }
        if (totalTime > MILLI_IN_HOURS) {
            val hrs:Long = (totalTime / MILLI_IN_HOURS)
            sb.append(" $hrs H")
            totalTime -= (hrs*MILLI_IN_HOURS)
        }
        if (totalTime > MILLI_IN_MIN) {
            val min:Long  = (totalTime / MILLI_IN_MIN)
            sb.append( "$min M")
            totalTime -= (min*MILLI_IN_MIN)
        }
        if (totalTime > MILLI_IN_SEC) {
            val sec:Long = totalTime / MILLI_IN_SEC
            sb.append(" $sec S")
            totalTime -= (sec*MILLI_IN_SEC)
        }
        if (totalTime > 0) {
            sb.append(" $totalTime ms")
        }
        return sb.toString()
    }

    @Test
    fun testReadCloudFileInChunks() {
        val s3Client = S3Client.builder().region(Region.of(awsConfig.region)).build()
        val req = GetObjectRequest.builder().bucket("cf-daart-exportfiles-dev").key("dhqp-2.ndjson").build()
        val resp = s3Client.getObject(req)
        val CHUNK_SIZE = 50 * 1024 * 1024 //5 Mb
        val totalTime = measureTimeMillis {
            var chunk: ByteArray = resp.readNBytes(CHUNK_SIZE)
            while (chunk.size > 0) {
                val timeInMillis = measureTimeMillis {
                    val newLines = String(chunk).countOccurrences()
                    print(" chunk (len: ${chunk.size}): new lines: $newLines ")
                    chunk = resp.readNBytes(CHUNK_SIZE)
                }
                println(" in ${timeInMillis}")
            }
        }
        println("total time: ${totalTime.format()}")
    }

    @Test
    fun testFormatTime() {
        println( 413083L.format())
    }
}