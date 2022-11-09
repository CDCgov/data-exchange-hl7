package gov.cdc.dex.logging


import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LogModelTest {

    val logger: Logger = LoggerFactory.getLogger(LogModelTest::class.java)
    @Test
    fun testLogModel() {
        val log = LogModel("LIB_UNIT", "INFO", "this is a test", "1234-5678", "noFileName")
//        println(log)
//        println(log.toJson())
        logger.error(log.toString())
    }
}