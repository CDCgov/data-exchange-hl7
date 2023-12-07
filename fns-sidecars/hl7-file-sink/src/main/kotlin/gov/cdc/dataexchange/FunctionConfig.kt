package gov.cdc.dataexchange

class FunctionConfig {
    val blobStorageContainerName: String = System.getenv("BlobStorageContainerName")
    val azureBlobProxy: AzureBlobProxy
    val blobStorageFolderName: String = System.getenv("BlobStorageFolderName")

    init {
        //Init Azure Storage connection
        val storageBlobConnStr = System.getenv("BlobStorageConnectionString")
        azureBlobProxy = AzureBlobProxy(storageBlobConnStr, blobStorageContainerName)
    }
}
