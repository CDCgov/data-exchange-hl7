package gov.cdc.dex.azure.cosmos

import com.azure.cosmos.models.PartitionKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.util.*

/**
 * Test class for CosmosClient.kt
 * @author QEH3@cdc.gov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("UNCHECKED_CAST")
class CosmosClientTest {

    companion object {
        private val logger = LoggerFactory.getLogger(CosmosClientTest::class.java.simpleName)

        private const val DATABASE_NAME = "hl7-tests"
        private const val CONTAINER_NAME = "unit-test"
        private val ENDPOINT = System.getenv("COSMOS_TEST_ENDPOINT")
        private val KEY = System.getenv("COSMOS_TEST_KEY")
        private const val PARTKEY_PATH = "/event/partition_key"
    }

    // using lateinit because it is singleton and only needs to be initialized once
    private lateinit var cosmosClient: CosmosClient

    @BeforeAll
    fun setUp() {
        cosmosClient = CosmosClient(DATABASE_NAME, CONTAINER_NAME, ENDPOINT, KEY, PARTKEY_PATH)
    }

    /**
     * Tests bulk operations.  bulkCreate, bulkUpsert, sqlReadItems functions in CosmosClient
     */
    @Test
    fun `Bulk operations`() {
        val itemList: MutableList<MutableMap<String, Any>> = mutableListOf()
        val bulkSize = 10
        try {
            for (i in 1..bulkSize) {
                itemList.add(generateItem(mockContent = "mockValue[$i]"))
            }
            // BULK CREATE
            cosmosClient.bulkCreate(itemList).blockLast()

            // READ items and compare to items sent
            for (item in itemList) {
                val id: String = item["id"] as String
                val partitionKey = PartKeyModifier(cosmosClient.getPartitionKeyPath()!!).read(item)!!
                val readItem = cosmosClient.readWithBlocking(id, partitionKey, Map::class.java) as Map<String, Any>
                assertItemEquals(item, readItem)
            }

            // prepare itemList for upsert
            itemList.map { item: MutableMap<String, Any> ->
                item["content"] = "${item["content"]}-upserted"
                item["upsertFlag"] = true
            }
            // BULK UPSERT
            cosmosClient.bulkUpsert(itemList).blockLast()

            // SQL READ
            val query = "SELECT * FROM c WHERE c.upsertFlag = true"
            var itemCount = 0
            (cosmosClient.sqlReadItems(query, Map::class.java) as Flux<Map<String, Any>>)
                .doOnNext { readItem: Map<String, Any> ->
                    ++itemCount
                    assert(readItem["upsertFlag"] as Boolean)
                }.doOnError {error ->
                    logger.error("Unable to read item: ${error.message}")
                }.doOnComplete {
                    logger.info("Completed bulk reading of $itemCount items")
                }.blockLast()
            assertEquals(bulkSize, itemCount)
        } catch (e: Exception) {
            fail("BULK FAIL: ${e.message}")
        } finally {
            for (item in itemList) {
                val id: String = item["id"] as String
                val partitionKey = PartKeyModifier(cosmosClient.getPartitionKeyPath()!!).read(item)!!
                cosmosClient.deleteWithBlocking(id, partitionKey)
                try {
                    assertItemNotFound(id, partitionKey)
                } catch (e: Exception) {
                    fail("DELETE FAIL: ${e.message}")
                }
            }
            cosmosClient.closeClient()
        }
    }

    /**
     * Tests CRUD (Create, Read, Update, Delete) functions of the Cosmos DB SDK.
     */
    @Test
    fun `CRUD operations`() {
        val itemId = UUID.randomUUID().toString()
        val partKeyStr = UUID.randomUUID().toString()
        val partitionKey = PartitionKey(partKeyStr)
        var item: Map<String, Any> = generateItem(itemId = itemId, partitionKey = partKeyStr)

        try {
            // CREATE
            cosmosClient.createWithBlocking(item)
            // READ
            var readItem = cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>
            assertItemEquals(item, readItem)
            // UPDATE
            item = generateItem(itemId, partKeyStr, mockContent = "mockUpdateValue")
            item["updateFlag"] = true
            cosmosClient.updateWithBlocking(item, itemId, partitionKey)
            // READ after update
            readItem = cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>
            assertItemEquals(item, readItem)
            assert(readItem["updateFlag"] as Boolean)
        } catch (e: Exception) {
            fail("FAIL: ${e.message}")
        } finally {
            cosmosClient.deleteWithBlocking(itemId, partitionKey)
            try {
                assertItemNotFound(itemId, partitionKey)
            } catch (e: Exception) {
                fail("FAIL: ${e.message}")
            }
            cosmosClient.closeClient()
        }
    }

    /**
     * Tests upserting a single record using Cosmos DB SDK
     */
    @Test
    fun `Upsert operation`() {
        val itemId = UUID.randomUUID().toString()
        val partKeyStr = UUID.randomUUID().toString()
        val partitionKey = PartitionKey(partKeyStr)
        var item: Map<String, Any> = generateItem(itemId = itemId, partitionKey = partKeyStr)

        try {
            // CREATE
            cosmosClient.createWithBlocking(item, partitionKey)
            assertItemEquals(item, cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>)
            // UPSERT with partition key
            item = generateItem(itemId = itemId, partitionKey = partKeyStr, mockContent = "mockUpsertValue")
            item["upsertFlag"] = true
            cosmosClient.upsertWithBlocking(item, partitionKey)
            var readResponse = cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>
            assertItemEquals(item, readResponse)
            assert(readResponse["upsertFlag"] as Boolean)

            // UPSERT without the partition key
            cosmosClient.upsertWithBlocking(item)
            readResponse = cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>
            assertItemEquals(item, readResponse)
            assert(readResponse["upsertFlag"] as Boolean)
        } catch (e: Exception) {
            fail("FAIL: ${e.message}")
        } finally {
            cosmosClient.deleteWithBlocking(itemId, partitionKey)
            try {
                assertItemNotFound(itemId, partitionKey)
            } catch (e: Exception) {
                fail("FAIL: ${e.message}")
            }
            cosmosClient.closeClient()
        }
    }

    /**
     * Tests whether an improperly initialized AsyncClient will handle exception correctly
     */
    @Test
    fun `Execute operation with improperly configured CosmosClient`() {
        val itemId = UUID.randomUUID().toString()
        val partKeyStr = UUID.randomUUID().toString()
        val partitionKey = PartitionKey(partKeyStr)
        val item = generateItem(itemId = itemId, partitionKey = partKeyStr)
        assertThrows<IllegalArgumentException> {
            val uninitializedClient = CosmosClient("mockDB", "mockContainer", "mockEndpoint", "mockKey", partitionKeyPath = "/mockPartitionKey")
            uninitializedClient.createWithBlocking(item, partitionKey)
        }
    }

     /* These helper functions generate a json map with id, partition key, and mock content */

    private fun generateItem(partitionKeyPath: String = cosmosClient.getPartitionKeyPath()!!, mockContent: String = "mockValue")
            : MutableMap<String, Any> {
        val itemId = UUID.randomUUID().toString()
        val partKeyStr = UUID.randomUUID().toString()
        val item: MutableMap<String, Any> = mutableMapOf("id" to itemId, "content" to mockContent)
        return PartKeyModifier(partitionKeyPath).write(item, partKeyStr)
    }

    private fun generateItem(itemId: String, partitionKey: String,
             partitionKeyPath: String = cosmosClient.getPartitionKeyPath()!!,  mockContent: String = "mockValue"): MutableMap<String, Any> {
        val item: MutableMap<String, Any> = mutableMapOf("id" to itemId, "content" to mockContent)
        return PartKeyModifier(partitionKeyPath).write(item, partitionKey)
    }

    /**
     * This helper functions compares items.
     */
    private fun assertItemEquals(expected: Map<*, *>, actual: Map<*, *>) {
        try {
            assertEquals(expected["id"], actual["id"]) // compare id
            val expectedPartKey =
                PartKeyModifier(cosmosClient.getPartitionKeyPath()!!).read(expected as Map<String, Any>)
            val actualPartKey =
                PartKeyModifier(cosmosClient.getPartitionKeyPath()!!).read(actual as Map<String, Any>)
            assertEquals(expectedPartKey, actualPartKey) // compare partition keys
            assertEquals(expected["content"], actual["content"]) // compare content
        } catch (e: Exception) {
            fail("FAIL: expected item and actual item do not match.\n${e.message}")
        }
    }

    /**
     * This helper function checks to see if an expected exception is thrown when you try to read an item that does not
     * exist.
     */
    private fun assertItemNotFound(itemId: String, partitionKey: PartitionKey) {
        try {
            assertThrows<Exception> { cosmosClient.readItem(itemId, partitionKey, Map::class.java).block() }
        } catch (e: Exception) {
            fail("FAIL: expected exception not thrown.\n${e.message}")
        }
    }
}