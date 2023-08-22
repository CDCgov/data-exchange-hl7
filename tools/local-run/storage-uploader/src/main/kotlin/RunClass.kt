import com.google.gson.JsonObject
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

object RunClass {
    @JvmStatic
    fun main(args: Array<String>) {
        var msgSourcePath = System.getenv("MSG_SOURCE_FOLDER")
        val msgOutputPath = System.getenv("MSG_OUTPUT_FOLDER")
        val msgType = System.getenv("MSG_TYPE")
        val route = System.getenv("ROUTE")

        if ( msgType == null || (msgType != "CASE" && msgType != "ELR")) {
            println("Environment variable MSG_TYPE should be set to ELR or CASE")
            return
        }
        if ( msgType == "ELR" && (route == null || route.isEmpty())) {
            println("Environment variable ROUTE is required if MSG_TYPE is ELR")
            return
        }
        if ( msgSourcePath == null) {
            println("Environment variable MSG_SOURCE_FOLDER should be set")
            return
        }
        if ( msgOutputPath == null) {
            println("Environment variable MSG_OUTPUT_FOLDER should be set")
            return
        }

        var count = 0

        Path("$msgSourcePath")
            .listDirectoryEntries("*.txt")
            .forEach { path ->
                count += 1

                val originalFileName = path.fileName.toString()
                val metadata = JsonObject().apply {
                    addProperty("message_type", msgType)
                    addProperty("original_filename", originalFileName)
                    addProperty("system_provider","LOCAL")
                    addProperty("reporting_jurisdiction","00")
                    if (msgType == "ELR") {
                        addProperty("route", route)
                    }
                }
                val props = JsonObject().apply {
                    addProperty("blob_size", Files.size(path))
                    add("metadata", metadata)
                }

                // extract file name and extension
                val extIndex = originalFileName.lastIndexOf('.')
                val fileNameOnly = if (extIndex != -1)
                    originalFileName.substring(0, extIndex)
                else originalFileName

                // write message and properties
                Path("$msgOutputPath/$originalFileName")
                    .writeText(File(path.absolutePathString()).readText())
                Path("$msgOutputPath/$fileNameOnly.properties").writeText(props.toString())

            }
        println(" $count message(s) processed")
    }
}

