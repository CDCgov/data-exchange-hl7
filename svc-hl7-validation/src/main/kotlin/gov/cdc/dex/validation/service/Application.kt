package gov.cdc.dex.validation.service

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "HL7v2 Validation API",
        version = "1.0",
        description = """# Introduction

The HL7v2 Validation API is designed to assist public health agencies in validating messages against the applicable profiles and standards as they prepare to submit their data to CDC.

 

The API is capable of validating a single message or a batch of roughly 100 messages. The purpose is for states and partners to be able to pre-test their messages to gain an understanding of where common errors lie. 

 

If a single message is submitted to the validator, The full report of errors is returned back to the caller. This will include the details of every error and warning found with the given message.

 

If multiple messages are submitted to the validator, a summary report of errors is returned. This summary will provide only counts of errors: how many total errors were found, and groupings of errors by type, category, path. It will also list how many errors were in each message.

 

By type -> this groups the errors by type. On the NIST profiles, there will be structure, content and value-set errors. This grouping will provide a list of the errors on each of those types for all messages. In the detailed report, entries will have an array of errors for each one of those types.

 

By Category -> each error in the NIST Profile is associated with a category. This grouping will provide a list of the errors on each category for all messages. In the detailed report, errors have a attribute "category" to indicate the category of this error. Possible values are "Constraint Error", "Usage", "Length", etc.

 

By Path -> each error in the detailed report indicates the HL7 path where the error happened. This can inform the segment, field, and possible component and subcomponent the error occurred."""

    )
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

