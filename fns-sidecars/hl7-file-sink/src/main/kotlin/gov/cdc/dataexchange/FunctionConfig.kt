package gov.cdc.dataexchange

class FunctionConfig {
    private val blobStorageContName: String = System.getenv("BlobStorageContainerName")
    val azureBlobProxy: AzureBlobProxy

    init {
        //Init Azure Storage connection
        val storageBlobConnStr = System.getenv("BlobStorageConnectionString")
        azureBlobProxy = AzureBlobProxy(storageBlobConnStr, blobStorageContName)
    }
}
