
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.model.ValidationIssue
import gov.cdc.dex.hl7.model.ValidationIssueCategoryType
import gov.cdc.dex.hl7.model.ValidationIssueType
import gov.cdc.dex.hl7.model.ValidationErrorMessage
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.ReportStatus


class MMGValidatorFnTest {


    @Test
    fun mmgValidatorNoIssues() {
        // Arrange
        val entries = emptyList<ValidationIssue>()
        val mmgReport = MmgReport(entries)

        // Act
        val result = MmgValidator()

        // Assert
        assertEquals(ReportStatus.MMG_VALID, result)
        assert(true)
    }

    @Test
    fun mmgValidatorWarningValidationIssues() {
        // Arrange
        val entries = listOf(
            ValidationIssue(
                classification = ValidationIssueCategoryType.WARNING,
                category = ValidationIssueType.DATA_TYPE,
                fieldName = "Field1",
                path = "\"/Lyme_WithMMGWarnings.txt\"",
                line = 1,
                errorMessage = ValidationErrorMessage.DATA_TYPE_MISMATCH,
                description = "Custom description"
            ),
            ValidationIssue(
                classification = ValidationIssueCategoryType.WARNING,
                category = ValidationIssueType.CARDINALITY,
                fieldName = "Field2",
                path = "\"/Lyme_WithMMGWarnings.txt\"",
                line = 2,
                errorMessage = ValidationErrorMessage.CARDINALITY_OVER,
                description = "Custom description"
            )
        )
        val mmgReport = MmgReport(entries)

        // Act
        val result = MmgValidator()

        // Assert
        assertEquals(ReportStatus.MMG_VALID, result)
        assert(true) // Add an assertion to ensure the test is executed
    }
    @Test
    fun mmgValidatorErrorValidationIssues() {
        // Arrange
        val entries = listOf(
            ValidationIssue(
                classification = ValidationIssueCategoryType.ERROR,
                category = ValidationIssueType.DATA_TYPE,
                fieldName = "Field1",
                path = "\"/Lyme_WithMMGErrors.txt\"",
                line = 1,
                errorMessage = ValidationErrorMessage.DATA_TYPE_MISMATCH,
                description = "mmgValidatorError description"
            ),
            ValidationIssue(
                classification = ValidationIssueCategoryType.ERROR,
                category = ValidationIssueType.CARDINALITY,
                fieldName = "Field2",
                path = "\"/Lyme_WithMMGErrors.txt\"",
                line = 2,
                errorMessage = ValidationErrorMessage.CARDINALITY_OVER,
                description = "Custom description"
            )
        )
        val mmgReport = MmgReport(entries)

        // Act
        val result = MmgValidator()

        // Assert
        assertEquals(ReportStatus.MMG_ERRORS, result)
        assert(true) // Add an assertion to ensure the test is executed
    }

}