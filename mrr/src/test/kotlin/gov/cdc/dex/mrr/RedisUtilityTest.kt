package gov.cdc.dex.mrr

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RedisUtilityTest {

    @Test
    fun redisConnection() {
        try {
            val key = "conditionv2:10049"
            val value = "{\"event_code\":10049,\"name\":\"West Nile virus non-neuroinvasive disease\",\"program\":\"NCEZID\",\"category\":\"Arboviral Diseases\",\"profiles\":[{\"name\":\"arbo_case_map_v1.0\",\"mmgs\":[\"mmg:arboviral_v1_3_2_mmg_20210721\"],\"special_cases\":[{\"applies_to\":\"group:legacy_arbo\",\"mmg_maps\":[\"mmg:arboviral_human_case_message_mapping_guide\"]}]}]}"
            val jedis = RedisUtility().redisConnection()
            val pipeline = jedis.pipelined()
            pipeline.set(key, value)
            pipeline.sync()
            pipeline.close()
        } catch (e: Exception) {
            println("Problem connecting to Redis:${e.printStackTrace()}")
        }
    }

}