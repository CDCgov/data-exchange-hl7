package gov.cdc.dataexchange

class FunctionConfig {
    val blobStorageContainerName: String = System.getenv("BlobStorageContainerName")
    val azureBlobProxy: AzureBlobProxy

    init {
        //Init Azure Storage connection
        val storageBlobConnStr = System.getenv("BlobStorageConnectionString")
        azureBlobProxy = AzureBlobProxy(storageBlobConnStr, blobStorageContainerName)
    }
}
