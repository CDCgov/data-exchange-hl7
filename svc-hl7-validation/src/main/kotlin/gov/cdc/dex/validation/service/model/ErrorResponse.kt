package gov.cdc.dex.validation.service.model

import java.time.Instant
import java.time.format.DateTimeFormatter


data class ErrorResponse(
    val http_status : Int,
    val timestamp : String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val error_message: String?
)
