package gov.cdc.dex.replay.service

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
@OpenAPIDefinition(
    info = Info(
        title = "HL7v2 Replay API",
        version = "1.0",
        description = "An API for replaying processed HL7v2 messages both validated and errored to HL7V2 Pipeline")
)
object Application {

    @JvmStatic
    fun main(args: Array<String>) {
//        Micronaut.run(Application.javaClass)
        Micronaut.build()
            .mainClass(Application.javaClass)
            .environmentPropertySource(true)
            .start()
    }
}


