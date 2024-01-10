package gov.cdc.dataexchange

import gov.cdc.dex.util.JsonHelper.toJsonElement
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Tag("UnitTest")
class StorageSinkTest {

    @Test
    fun testSaveBlob() {
        val message = "This is the content I am going to save"
        val function = Function()
        val newMetadata = mutableMapOf<String, String>()
        newMetadata["meta_destination_id"] = "dex-hl7"

        newMetadata["meta_ext_event"] = Function.fnConfig.blobStorageFolderName
        val today = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())
        function.saveBlobToContainer("test.txt", message, newMetadata)
    }

}