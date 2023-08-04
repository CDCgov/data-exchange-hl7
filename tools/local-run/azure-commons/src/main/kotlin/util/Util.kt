package util

// parse file path in fileName and extension
fun parseFileName(filePath:String ): Pair<String,String> {
    val fileNameWithExt = filePath.substringAfterLast("/")
    val extIndex = fileNameWithExt.lastIndexOf('.')
    return Pair(fileNameWithExt.substring(0, extIndex), fileNameWithExt.substring(extIndex))
}

