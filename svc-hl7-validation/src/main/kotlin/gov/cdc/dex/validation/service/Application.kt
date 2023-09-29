package gov.cdc.dex.validation.service

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "HL7 VALIDATION API",
        version = "1.0",
        description = "An API for validating HL7 messages")
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

