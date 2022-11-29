package gov.cdc.dex.hl7.model

 class MmgReport(override val entries: List<ValidationIssue>?) : ValidationReport(entries)