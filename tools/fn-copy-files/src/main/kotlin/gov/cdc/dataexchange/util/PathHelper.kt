package gov.cdc.dataexchange.util

/**
 * Transform dot path format to slash format path string
 */
class PathHelper(private val dotPath: String) {
    fun transform() = if (dotPath != "ROOT") { dotPath.replace('.', '/') } else ""
}