package cdc.ede.hl7.structuralvalidator

import gov.nist.validation.report.Report

case class HL7MessageMetadata(filePath: String,
                                fileName: String, 
                                fileTimestamp: String, 
                                fileSize: Long, 
                                fileUUID: String, 
                                fileEventID: String, 
                                fileEventTimestamp: String, 
                                messageUUID: String,
                                messageIndex: Int)


case class HL7Message(content: String, metadata: HL7MessageMetadata)

                     
case class HL7MessageOut(content: String, metadata: HL7MessageMetadata, structuralValidationReport: Report)
