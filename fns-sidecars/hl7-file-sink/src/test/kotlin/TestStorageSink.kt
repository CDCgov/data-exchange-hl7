package gov.cdc.dataexchange

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TestStorageSink {

    @Test
    fun testSaveBlob() {
        val message = "This is the content I am going to save"
        val function = Function()
        val today = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())
        function.saveBlobToContainer("$today/test.txt", message)
    }

}