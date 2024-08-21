package cdc.gov

import org.junit.jupiter.api.Test

internal class TestTransformerTest {

    @Test
    fun transformProfile() {
        val transformer = IgamtToBumblebeeTransformer()
        transformer.transformProfile("./src/test/resources/PROFILE.xml", "./src/test/resources/output", "Phinspec")

    }
}