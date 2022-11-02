package gov.cdc.dex.hl7

import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.hl7.model.ValidationIssue
import gov.cdc.dex.hl7.model.ValidationIssueCategoryType

class MmgReport(private val report: List<ValidationIssue>) {
    // companion object {
    // }

    fun getReportStatus(): ReportStatus {

      var reportStatus = ReportStatus.MMG_VALID
      run reportStatusCheck@ {
          report.forEach { issue: ValidationIssue -> 
              when (issue.category) {
                  ValidationIssueCategoryType.ERROR -> {
                      reportStatus = ReportStatus.MMG_ERRORS
                      return@reportStatusCheck
                  }
                //   ValidationIssueCategoryType.WARNING -> {
                //       reportStatus = ReportStatus.MMG_WARNINGS
                //       return@reportStatusCheck
                //   }
                  else -> { /* keep looking */ }
              }
          } // .validationReportFull.forEach
      } // .run

        return reportStatus
    } // .getReportStatus

} // .MmgReporter