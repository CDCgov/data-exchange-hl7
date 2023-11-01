package gov.cdc.dex.validation.service.model

data class StructureErrorInfo (
    val line: Int = 0,
    val column: Int = 0,
    val path : String = "",
    val description : String,
    val category : String = "Runtime Error",
    val classification : String = "Error"
)
