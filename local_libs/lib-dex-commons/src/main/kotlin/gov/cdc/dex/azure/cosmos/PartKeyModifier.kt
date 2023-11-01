package gov.cdc.dex.azure.cosmos

import com.azure.cosmos.models.PartitionKey

/**
 * Gives access to the partition key of an item.  You can read and write a
 * partition key, providing the partition key path
 * @author QEH3@cdc.gov
 * @param partitionKeyPath
 */
class PartKeyModifier(private val partitionKeyPath: String) {

    /**
     * Read the partition key in a given item
     * @param item
     * @return PartitionKey?
     */
    fun read(item: Map<String, Any>): PartitionKey? {
        val mutableItem: MutableMap<String, Any> = item.toMutableMapDeep()
        val pathElements = partitionKeyPath.split("/").filterNot { it.isEmpty() }

        var currentValue: Any? = mutableItem
        for (key in pathElements) {
            currentValue = (currentValue as? Map<String, Any>)?.get(key)
            if (currentValue == null) {
                return null
            }
        }
        return PartitionKey(currentValue)
    }

    /**
     * Write the partition key to an item.
     * @param item
     * @param partKeyStr
     * @return updated item
     */
    fun write(item: Map<String, Any>, partKeyStr: String): MutableMap<String, Any> {
        val mutableItem: MutableMap<String, Any> = item.toMutableMapDeep()
        val pathElements = partitionKeyPath.split("/").filterNot { it.isEmpty() }

        var currentMap: MutableMap<String, Any> = mutableItem
        for (i in pathElements.indices) {
            val pathElement = pathElements[i]
            if (i == pathElements.size - 1) {
                currentMap[pathElement] = partKeyStr
            } else {
                if (!currentMap.containsKey(pathElement) || currentMap[pathElement] !is MutableMap<*, *>) {
                    currentMap[pathElement] = mutableMapOf<String, Any>()
                }
                currentMap = currentMap[pathElement] as MutableMap<String, Any>
            }
        }
        return mutableItem
    }

    /**
     * helper function to create a MutableMap<String, Any>
     */
    private fun Map<*, *>.toMutableMapDeep(): MutableMap<String, Any> {
        val mutableMap = this.toMutableMap()
        for ((key, value) in mutableMap) {
            if (value is Map<*, *>) {
                mutableMap[key] = value.toMutableMapDeep()
            }
        }
        return mutableMap as MutableMap<String, Any>
    }
}