import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

object RunClass {
    @JvmStatic
    fun main(args: Array<String>) {
        val gson = GsonBuilder().serializeNulls().create()
        var folders = JsonObject().apply {
            collectFolder(this, "hl7-recdeb-ok")
            collectFolder(this, "hl7-recdeb-err")
            collectFolder(this, "hl7-redacted-ok")
            collectFolder(this, "hl7-redacted-err")
            collectFolder(this, "hl7-structure-ok")
            collectFolder(this, "hl7-structure-err")
            collectFolder(this, "hl7-json-lake-ok")
            collectFolder(this, "hl7-json-lake-err")
            collectFolder(this, "hl7-lake-segments-ok")
            collectFolder(this, "hl7-lake-segments-err")
        }
        Path("../js/hubs.js")
            .writeText("let hubs = ${gson.toJson(folders)}")
    }
    private fun collectFolder(o:JsonObject, folderName:String) {
        val files = JsonObject()
        Path("../event-hubs/$folderName")
            .listDirectoryEntries("*.*")
            .filter {it.fileName.toString().indexOf(".properties") == -1 }
            .forEach { files.addProperty(it.fileName.toString(), "") }
        o.add( folderName, files)
    }
}

