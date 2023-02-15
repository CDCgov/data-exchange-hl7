package gov.cdc.dex.mrr

import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

object ClientUtils {
    fun getResourcePath(directoryName: String) : Path {
        val url = Thread.currentThread().contextClassLoader.getResource(directoryName)?.toURI()
            ?: throw Exception("Directory '$directoryName' not found.")
        return if (url.scheme.equals("jar")) {
            val fs = try {
                FileSystems.newFileSystem(url, emptyMap<String, Any>())
            } catch (e : FileSystemAlreadyExistsException) {
                FileSystems.getFileSystem(url)
            }
            fs.getPath(directoryName)
        } else {
            Paths.get(url)
        }
    }
}