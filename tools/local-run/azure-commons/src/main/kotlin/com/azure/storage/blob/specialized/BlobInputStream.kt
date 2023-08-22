package com.azure.storage.blob.specialized

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class BlobInputStream(f:File): FileInputStream(f) {
}