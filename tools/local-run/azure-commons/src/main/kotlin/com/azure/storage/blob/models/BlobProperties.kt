package com.azure.storage.blob.models

import java.time.OffsetDateTime

class BlobProperties(
    var metadata:Map<String, String>,
    var lastModified: OffsetDateTime,
    val blobSize:Long,
    val blobName:String) {
}